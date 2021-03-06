/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.samoa.flink.topology.impl;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.samoa.core.EntranceProcessor;
import org.apache.samoa.flink.helpers.Utils;
import org.apache.samoa.topology.AbstractEntranceProcessingItem;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.io.Serializable;

public class FlinkEntranceProcessingItem extends AbstractEntranceProcessingItem
		implements FlinkComponent, Serializable {

	private transient StreamExecutionEnvironment env;
	private transient DataStream outStream;


	public FlinkEntranceProcessingItem(StreamExecutionEnvironment env, EntranceProcessor proc) {
		super(proc);
		this.env = env;
	}

	@Override
	public void initialise() {
		final EntranceProcessor proc = getProcessor();
		final String streamId = getOutputStream().getStreamId();
		final int compID = getComponentId();

		
		outStream = env.addSource(new RichSourceFunction() {

			private volatile boolean isCancelled;
			
			@Override
			public void run(SourceContext sourceContext) throws Exception {
				while(!isCancelled && entrProc.hasNext())
				{
					sourceContext.collect(SamoaType.of(entrProc.nextEvent(), id));
				}
			}

			@Override
			public void cancel() {
				isCancelled = true;
			}

			EntranceProcessor entrProc = proc;
			String id = streamId;

			@Override
			public void open(Configuration parameters) throws Exception {
				super.open(parameters);
				entrProc.onCreate(compID);
			}
			
		}).returns(Utils.tempTypeInfo);

		((FlinkStream) getOutputStream()).initialise();
	}


	@Override
	public boolean canBeInitialised() {
		return true;
	}

	@Override
	public boolean isInitialised() {
		return outStream != null;
	}

	@Override
	public int getComponentId() {
		return -1; // dummy number shows that it comes from an Entrance PI
	}

	@Override
	public DataStream getOutStream() {
		return outStream;
	}
}
