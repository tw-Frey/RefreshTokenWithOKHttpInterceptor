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

這個專案是要實驗  
當 Dispatcher 的 [maxRequests](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dispatcher/max-requests) 和 [maxRequestsPerHost](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dispatcher/max-requests-per-host) 設為 1 時  
以下條件是否真能達成限制效果：要 FIFO 佇列 一次只執行一個 Request  

maxRequests = 1<br>maxRequestsPerHost = 1|多個 clients|單個 client
:-:|:-:|:-:
異步(async) Request|_無效_|**有效**
同步(sync) Request|_無效_|_無效_

最後要檢視在 [Retrofit](https://square.github.io/retrofit) 是否結果一致  
(有 [issue](https://github.com/square/retrofit/issues/1936) 表示在 Retrofit 會失效, 需要再增加設定)

---

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
前八條是併發 ([httpRequest](app/src/main/java/tw/idv/fy/okhttp/interceptor/refreshtoken/HttpRequestRepository.kt#L56))  
後八條是佇列 ([fetchTokenRequest](app/src/main/java/tw/idv/fy/okhttp/interceptor/refreshtoken/HttpRequestRepository.kt#L56))  
httpRequest 幾乎同時間發出  
但受制於 Interceptor 裡的 fetchTokenRequest 由 Dispatcher 發配執行  
因為 maxRequests 和 maxRequestsPerHost 都設為 1  
相當於 佇列排隊 一個接一個 執行
