package com.github.zlzhang0122.ispark.client

import com.github.zlzhang0122.ispark.ISparkRpcConf
import com.github.zlzhang0122.ispark.rpc.netty.NettyRpcEnvFactory
import com.github.zlzhang0122.ispark.rpc.{RpcAddress, RpcEndpointRef, RpcEnv, RpcEnvClientConfig}
import com.github.zlzhang0122.ispark.server.{HelloEndpoint, SayHello}

import scala.concurrent.ExecutionContext.Implicits._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * client
  *
  * @Author: zlzhang0122
  * @Date: 2021/1/16 5:05 下午
  */
object SimpleClientTest {

  def main(args: Array[String]): Unit = {
    val config = RpcEnvClientConfig(new ISparkRpcConf(), "hello-client")
    val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)
    val endPointRef: RpcEndpointRef = rpcEnv.setupEndpointRef(RpcAddress("localhost", 52345), HelloEndpoint.ENDPOINT_NAME)
    val future: Future[String] = endPointRef.ask[String](SayHello("abc"))
    future.onComplete {
      case scala.util.Success(value) => println(s"Got the result = $value")
      case scala.util.Failure(e) => e.printStackTrace
    }
    Await.result(future, Duration.apply("30s"))
  }
}
