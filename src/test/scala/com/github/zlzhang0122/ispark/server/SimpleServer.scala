/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.zlzhang0122.ispark.server

import com.github.zlzhang0122.ispark.ISparkRpcConf
import com.github.zlzhang0122.ispark.rpc.netty.NettyRpcEnvFactory
import com.github.zlzhang0122.ispark.rpc.{RpcCallContext, RpcEndpoint, RpcEnv, RpcEnvServerConfig, ThreadSafeRpcEndpoint}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * server
  *
  * @Author: zlzhang0122
  * @Date: 2021/1/16 5:05 下午
  */
object RpcServerTest {

  def main(args: Array[String]): Unit = {
    val config = RpcEnvServerConfig(new ISparkRpcConf(), "hello-server", "localhost", 52345)
    val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)
    val helloEndpoint: RpcEndpoint = new HelloEndpoint(rpcEnv)
    val helloEndpointRef = rpcEnv.setupEndpoint(HelloEndpoint.ENDPOINT_NAME, helloEndpoint)
    val f = Future {
      val future: Future[String] = helloEndpointRef.ask[String](SayHello("abc"))
      future.onComplete {
        case scala.util.Success(value) => println(s"client got result => $value")
        case scala.util.Failure(e) => e.printStackTrace()
      }
    }
    Await.result(f, Duration.apply("240s"))
    println("waiting to be called...")
    rpcEnv.awaitTermination()
  }

}

class HelloEndpoint(realRpcEnv: RpcEnv) extends ThreadSafeRpcEndpoint {

  override def onStart(): Unit = {
    println("start hello endpoint")
  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    // Messages sent and received locally
    case SayHello(msg) => {
      println(s"receive $msg")
      context.reply(msg.toUpperCase)
    }
  }

  override def onStop(): Unit = {
    println("stop hello...")
  }

  /**
    * The [[RpcEnv]] that this [[RpcEndpoint]] is registered to.
    */
  override val rpcEnv: RpcEnv = realRpcEnv
}

object HelloEndpoint {
  val ENDPOINT_NAME = "my-hello"
}

case class SayHello(msg: String)
