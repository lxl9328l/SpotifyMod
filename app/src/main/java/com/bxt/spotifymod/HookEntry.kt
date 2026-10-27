                    val cl = lpparam.classLoader
                    
                    var premMsg = ""
                    runCatching { PremiumPatcher.patch(cl) }
                        .onSuccess { premMsg = "PremiumPatcher 已跑" }
                        .onFailure { premMsg = "Premium 报错: ${it.message}" }
                        
                    var adMsg = ""
                    runCatching { AdPatcher.patch(cl) }
                        .onSuccess { adMsg = "AdPatcher 已跑" }
                        .onFailure { adMsg = "Ad 报错: ${it.message}" }
                        
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            param.thisObject as android.content.Context,
                            "$premMsg\n$adMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
