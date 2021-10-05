有一個需求  
透過 [OkHttp](https://square.github.io/okhttp) 同時打同一隻 API 多次 (例如 15 次)  
打 API 時會加 [Interceptor](https://square.github.io/okhttp/interceptors)  
在 Interceptor 裡會打另一隻 API  
需求的是：在 Interceptor 打 API 時, 要 FIFO 佇列方式 執行, 且一次只能執行一個 [Request](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-request)  

通常在 Interceptor 打 API 會時以 _**同步 (sync)**_ 方式執行  
而想要 FIFO 佇列 一次只執行一個 Request  
會想到可以應用 [Dispatcher](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dispatcher)  
但是根據官方文件： _**Dispatcher** can be used that Policy on when **async** requests are executed._  

---

這個專案是要實驗(POC)  
當 Dispatcher 的 [maxRequests](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dispatcher/max-requests) 和 [maxRequestsPerHost](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dispatcher/max-requests-per-host) 設為 1 時  
以下條件是否真能達成限制效果：要 FIFO 佇列 一次只執行一個 Request  

最後要檢視在 [Retrofit](https://square.github.io/retrofit) 是否結果一致  
(有 [issue](https://github.com/square/retrofit/issues/1936) 表示在 Retrofit 會失效, 需要再增加設定)  
***[補充] 已經完成套用 [Retrofits2](https://github.com/tw-Frey/RefreshTokenWithOKHttpInterceptor/releases/tag/Retrofit2) , 目前沒有問題(還在預期POC內)***   
***[原因] 因為專案沒有用到 RxJava 更別說用到 RxJava Schedulers***

---

結果如下  
maxRequests = 1<br>maxRequestsPerHost = 1|多個 clients|單個 client
:-:|:-:|:-:
異步(async) Request|_無效_|**有效**
同步(sync) Request|_無效_|_無效_

\
![/images/單個client異步request.png](/images/單個client異步request.png)
#### 只有 單個client異步request 有效 ####

---

因為在 [Interceptor](https://square.github.io/okhttp/interceptors) 打 API 會時以 _**同步 (sync)**_ 方式執行  
但 [Dispatcher](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dispatcher) 只有 _**同步 (sync)**_ 有效  
也就是說要寫 異步 實現 同步  
方法是不少, 例如 Furture 或 Deferred 或 Promise 甚至 Coroutine  
這個專案是使用 Kotlin Coroutine 的 Flow/callbackFlow 來實現  

結果如下

![/images/image1.png](/images/image1.png)
![/images/image2.png](/images/image2.png)

上圖裡  
前八條是併發 ([httpRequest](app/src/main/java/tw/idv/fy/okhttp/interceptor/refreshtoken/repository/HttpRequestRepository.kt#L62))  
後八條是佇列 ([fetchTokenRequest](app/src/main/java/tw/idv/fy/okhttp/interceptor/refreshtoken/repository/TokenRepository.kt#L41))  
httpRequest 幾乎同時間發出  
但受制於 Interceptor 裡的 fetchTokenRequest 由 Dispatcher 發配執行  
因為 maxRequests 和 maxRequestsPerHost 都設為 1  
相當於 佇列排隊 一個接一個 執行

---

### 處理 Exception 和 Invalid/Expired Token ###

這個專案使用 Coroutine/Flow  
當 Exception 發生時, 要用 Coroutine 機制來處理  
特別注意是，透過 close/cancel 傳遞 Exception 時  
目前只有 CancellationException 可以顯示出來    
其他 Exception 會被吃掉  
這是現在 Kotlin/Coroutine 機制  
說不定以後會改變  

當 Token 發生 Invalid(錯誤代碼1002) / Expired(錯誤代碼13)    
要將結果(錯誤代碼) 傳遞(客製) 出去  
由於只有 [Application Interceptors](https://square.github.io/okhttp/interceptors/#application-interceptors) 可以直接客製 response (不用執行 chain.poccess)  
目前將 [fetchTokenRequest](app/src/main/java/tw/idv/fy/okhttp/interceptor/refreshtoken/repository/TokenRepository.kt#L41) 放到 Application Interceptors 裡  
結果如下  

![/images/image3.png](/images/image3.png)

[httpRequest](app/src/main/java/tw/idv/fy/okhttp/interceptor/refreshtoken/repository/HttpRequestRepository.kt#L62) 也發生佇列排隊現象(幾乎)  

如果將 fetchTokenRequest 放到 [Network Interceptors](https://square.github.io/okhttp/interceptors/#network-interceptors) 裡  
結果如下

![/images/image4.png](/images/image4.png)

每一個 getData 後者比較長 且 比較早 start 並 更符合併發期待  
但實際上 total 花費時間是差不多的  

目前想到一個可能解釋: Android Profile 的 network 計算時間方式 排除 Application Interceptors  
所以前者的 getToken 的時間區間 不包含於 getData 時間區間內  

由於 [Network Interceptors](https://square.github.io/okhttp/interceptors/#network-interceptors) 內至少應執行一次 chain.proccess  
都已經 Invalid/Expired 再打 api 似乎是浪費流量  

已經想到一個方式可以應用 [Network Interceptors](https://square.github.io/okhttp/interceptors/#network-interceptors) 且避免在 Invalid/Expired 時再打 api  
就是在 getToken 加入應用 [OKHttp/Http Cached](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-cache/) 機制  

chain.proccess 的 request 只限制 same domain 和 same port  
本來 getToken 跟 getData 就是 同協同域同埠 api  
而且返回 getToken 的 Invalid/Expired 結果(response) 一樣滿足 專案/流程 需求  

因為原本 getToken 被 retrofit/OkHttp 包裝成異步 且在 fetchTokenRequest 裡   
但 chain.proccess 是 同步 且在 httpRequest 裡 (跟上述有不同的 client)  
所以得應用 cached 機制： 在 getDate 取得 getToken 的 response  
(疑問: OkHttp 的 cached 是 Thread-Safe 嗎？)  

不過現階段不做這件事  
因為目前 POC 結果差不多完備  
可以導入到 需求的專案(東XApp)  
之後再補 Network Interceptors + Cached 的 POC  

---

實作 IsTokenExpired  (待續)
