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

package com.github.zlzhang0122.ispark.rpc

import java.io.File

import com.github.zlzhang0122.ispark.ISparkRpcConf
import com.github.zlzhang0122.ispark.rpc.netty.NettyRpcEnvFactory
import com.github.zlzhang0122.ispark.util.RpcUtils

import scala.concurrent.Future

/**
  * A RpcEnv implementation must have a [[RpcEnvFactory]] implementation with an empty constructor
  * so that it can be created via Reflection.
  *
  * @Author: zlzhang0122
  * @Date: 2021/1/16 3:17 下午
  */
object RpcEnv {

  def create(
              name: String,
              host: String,
              port: Int,
              conf: ISparkRpcConf,
              securityManager: SecurityManager,
              clientMode: Boolean = false): RpcEnv = {
    create(name, host, host, port, conf, securityManager, 0, clientMode)
  }

  def create(
              name: String,
              bindAddress: String,
              advertiseAddress: String,
              port: Int,
              conf: ISparkRpcConf,
              securityManager: SecurityManager,
              numUsableCores: Int,
              clientMode: Boolean): RpcEnv = {
    val config = RpcEnvCustomConfig(conf, name, bindAddress, advertiseAddress, port,
      securityManager, numUsableCores, clientMode)
    NettyRpcEnvFactory.create(config)
  }
}


/**
  * An RPC environment. [[RpcEndpoint]]s need to register itself with a name to [[RpcEnv]] to
  * receives messages. Then [[RpcEnv]] will process messages sent from [[RpcEndpointRef]] or remote
  * nodes, and deliver them to corresponding [[RpcEndpoint]]s. For uncaught exceptions caught by
  * [[RpcEnv]], [[RpcEnv]] will use [[RpcCallContext.sendFailure]] to send exceptions back to the
  * sender, or logging them if no such sender or `NotSerializableException`.
  *
  * [[RpcEnv]] also provides some methods to retrieve [[RpcEndpointRef]]s given name or uri.
  */
abstract class RpcEnv(conf: ISparkRpcConf) {

  val defaultLookupTimeout = RpcUtils.lookupRpcTimeout(conf)

  /**
    * Return RpcEndpointRef of the registered [[RpcEndpoint]]. Will be used to implement
    * [[RpcEndpoint.self]]. Return `null` if the corresponding [[RpcEndpointRef]] does not exist.
    */
  private[rpc] def endpointRef(endpoint: RpcEndpoint): RpcEndpointRef

  /**
    * Return the address that [[RpcEnv]] is listening to.
    */
  def address: RpcAddress

  /**
    * Register a [[RpcEndpoint]] with a name and return its [[RpcEndpointRef]]. [[RpcEnv]] does not
    * guarantee thread-safety.
    */
  def setupEndpoint(name: String, endpoint: RpcEndpoint): RpcEndpointRef

  /**
    * Retrieve the [[RpcEndpointRef]] represented by `uri` asynchronously.
    */
  def asyncSetupEndpointRefByURI(uri: String): Future[RpcEndpointRef]

  /**
    * Retrieve the [[RpcEndpointRef]] represented by `uri`. This is a blocking action.
    */
  def setupEndpointRefByURI(uri: String): RpcEndpointRef = {
    defaultLookupTimeout.awaitResult(asyncSetupEndpointRefByURI(uri))
  }

  /**
    * Retrieve the [[RpcEndpointRef]] represented by `address` and `endpointName`.
    * This is a blocking action.
    */
  def setupEndpointRef(address: RpcAddress, endpointName: String): RpcEndpointRef = {
    setupEndpointRefByURI(RpcEndpointAddress(address, endpointName).toString)
  }

  /**
    * Stop [[RpcEndpoint]] specified by `endpoint`.
    */
  def stop(endpoint: RpcEndpointRef): Unit

  /**
    * Shutdown this [[RpcEnv]] asynchronously. If need to make sure [[RpcEnv]] exits successfully,
    * call [[awaitTermination()]] straight after [[shutdown()]].
    */
  def shutdown(): Unit

  /**
    * Wait until [[RpcEnv]] exits.
    *
    * TODO do we need a timeout parameter?
    */
  def awaitTermination(): Unit

  /**
    * [[RpcEndpointRef]] cannot be deserialized without [[RpcEnv]]. So when deserializing any object
    * that contains [[RpcEndpointRef]]s, the deserialization codes should be wrapped by this method.
    */
  def deserialize[T](deserializationAction: () => T): T
}

abstract class RpcEnvConfig() {
  def conf: ISparkRpcConf

  def name: String

  def bindAddress: String

  def advertiseAddress: String

  def port: Int

  def securityManager: SecurityManager

  def numUsableCores: Int

  def clientMode: Boolean
}

case class RpcEnvCustomConfig(conf: ISparkRpcConf,
                              name: String,
                              bindAddress: String,
                              advertiseAddress: String,
                              port: Int,
                              securityManager: SecurityManager,
                              numUsableCores: Int,
                              clientMode: Boolean
                             ) extends RpcEnvConfig;

case class RpcEnvServerConfig(conf: ISparkRpcConf,
                              name: String,
                              bindAddress: String,
                              port: Int
                             ) extends RpcEnvConfig {
  override def advertiseAddress: String = bindAddress

  override def numUsableCores: Int = 2

  override def clientMode: Boolean = false

  override def securityManager: SecurityManager = null
}

case class RpcEnvClientConfig(conf: ISparkRpcConf,
                              name: String) extends RpcEnvConfig {
  override def bindAddress: String = null

  override def advertiseAddress: String = null

  override def port: Int = 0

  override def numUsableCores: Int = 2

  override def clientMode: Boolean = true

  override def securityManager: SecurityManager = null
}

