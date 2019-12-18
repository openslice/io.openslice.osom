/*-
 * ========================LICENSE_START=================================
 * io.openslice.osom
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.openslice.osom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.so641.model.ServiceOrder;

public class SCMocked {


	private static final transient Log logger = LogFactory.getLog(SCMocked.class.getName());
	
	/**
	 * get mocked service order by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public String getOrderById(String id) throws IOException {
		logger.info( "getOrderById id= " + id );
		File sspec = new File( "src/test/resources/TestExServiceOrder.json" );
		InputStream in = new FileInputStream( sspec );
		String sspectext = IOUtils.toString(in, "UTF-8");
		
		return sspectext;
	}
	
	
	/**
	 * 
	 * get mocked service spec by id from model via bus
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public String getSpecById(String id)  throws IOException {

		logger.info( "getSpecById id= " + id );
		String sspectext = null;
		if ( id.equals( "d8515f8f-786c-432b-9a74-5037e54c7974" )) {
			File sspec = new File( "src/test/resources/TestExBundleSpec.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "59d08753-e1b1-418b-9e3e-d3a3bb573051" )) {
			File sspec = new File( "src/test/resources/TestExSpec1.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "a7ff2349-ccab-42a1-9212-51d4a1912247" )) {
			File sspec = new File( "src/test/resources/TestExSpec2.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "81a08735-e1b1-418b-9e3e-d3a3bb573007" )) {
			File sspec = new File( "src/test/resources/TestExSpec3.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		} else if ( id.equals( "93b9928c-de35-4495-a157-1100f6e71c92" )) {
			File sspec = new File( "src/test/resources/TestServiceOrderDates.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		}
		
		return sspectext;
	}
	

	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }
}
