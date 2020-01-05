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

import io.openslice.model.DeploymentDescriptor;
import io.openslice.model.DeploymentDescriptorStatus;
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
		} else if ( id.equals( "c00446ac-c8af-47ad-ac94-518d4bdd4c13" )) {
			File sspec = new File( "src/test/resources/TestServiceNSD.json" );
			InputStream in = new FileInputStream( sspec );
			sspectext = IOUtils.toString(in, "UTF-8");			
		}
		
		
		
		return sspectext;
	}
	
	public String getMockedService()  throws IOException {
		logger.info( "getMockedService()"  );

		String sspectext = null;
		
		File sspec = new File( "src/test/resources/TestService.json" );
		InputStream in = new FileInputStream( sspec );
		sspectext = IOUtils.toString(in, "UTF-8");
		return sspectext;		
	}
	
	public String updateServiceOrder(String so)  throws IOException {

		logger.info( "updateServiceOrder so= " + so );
		String sspectext = null;
		
		File sspec = new File( "src/test/resources/TestService.json" );
		InputStream in = new FileInputStream( sspec );
		sspectext = IOUtils.toString(in, "UTF-8");
		return sspectext;		
	}

	
	public String getServiceById(String id)  throws IOException {

		logger.info( "getServiceById id = " + id );
		String sspectext = null;
		
		File sspec = new File( "src/test/resources/TestService.json" );
		InputStream in = new FileInputStream( sspec );
		sspectext = IOUtils.toString(in, "UTF-8");
		return sspectext;		
	}
	
	public String req_deploy_nsd( String ddreq )  throws IOException {

		logger.info( "ddreq getExperiment = " + toJsonString(ddreq) );
		
		DeploymentDescriptor ddresp = toJsonObj( ddreq, DeploymentDescriptor.class);
		ddresp.setId(123456789);
		return toJsonString(ddresp);		
	}
	
	
	public String req_deployment_id( Long ddreqId )  throws IOException {

		logger.info( "ddreq get id = " + ddreqId );
		
		DeploymentDescriptor ddresp = new DeploymentDescriptor();
		ddresp.setId(ddreqId);
		ddresp.setStatus( DeploymentDescriptorStatus.RUNNING );
		return toJsonString(ddresp);		
	}
	
	 static String toJsonString(Object object) throws IOException {
	        ObjectMapper mapper = new ObjectMapper();
	        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	        return mapper.writeValueAsString(object);
	    }

	static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.readValue( content, valueType);
    }
}
