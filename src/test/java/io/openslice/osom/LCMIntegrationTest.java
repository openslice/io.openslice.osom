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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.RepositoryService;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.BeforeTestExecution;

import io.openslice.osom.lcm.LCMRulesExecutor;
import io.openslice.osom.lcm.LCMRulesExecutorVariables;
import io.openslice.osom.lcm.LcmBaseExecutor;
import io.openslice.tmf.common.model.Any;
import io.openslice.tmf.common.model.EValueType;
import io.openslice.tmf.common.model.service.Characteristic;
import io.openslice.tmf.lcm.model.LCMRuleSpecification;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristic;
import io.openslice.tmf.scm633.model.ServiceSpecCharacteristicValue;
import io.openslice.tmf.scm633.model.ServiceSpecification;
import io.openslice.tmf.sim638.model.ServiceCreate;
import io.openslice.tmf.so641.model.ServiceOrder;

@ExtendWith(FlowableSpringExtension.class)
@SpringBootTest(properties = { "CATALOG_GET_SERVICEORDER_BY_ID = direct:get_mocked_order",
		"CATALOG_GET_SERVICESPEC_BY_ID = direct:get_mocked_spec", "CATALOG_ADD_SERVICE = direct:get_mocked_add_service",
		"CATALOG_UPD_SERVICEORDER_BY_ID = direct:get_mocked_upd_order",
		"CATALOG_ADD_SERVICEORDER = direct:get_mocked_upd_order",
		"CATALOG_GET_SERVICE_BY_ID = direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEMS_GET: direct:get_mocked_service_queueitems",
		"CATALOG_SERVICE_QUEUE_ITEM_UPD: direct:get_mocked_service_id",
		"CATALOG_SERVICE_QUEUE_ITEM_DELETE: direct:get_mocked_service_id",
		"CATALOG_UPD_SERVICE = direct:get_mocked_upd_service", 
		"NFV_CATALOG_DEPLOY_NSD_REQ = direct:req_deploy_nsd",
		"CATALOG_GET_LCMRULE_BY_ID = direct:get_mocked_lcmrulebyid",		
		"CATALOG_GET_LCMRULES_BY_SPECID_PHASE = direct:get_mocked_lcmrulesbyspecid",				
		"CATALOG_SERVICES_OF_PARTNERS = direct:get_mocked_service_queueitems",			
		"CATALOG_SERVICES_TO_TERMINATE = direct:get_mocked_service_queueitems",	
		"CATALOG_GET_SERVICEORDER_IDS_BY_STATE = direct:get_mocked_service_queueitems",		
		"ALARMS_ADD_ALARM=mock:output",
		"ALARMS_UPDATE_ALARM=mock:output",
		"ALARMS_GET_ALARM=mock:output",
		"NFV_CATALOG_GET_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		"NFV_CATALOG_UPD_DEPLOYMENT_BY_ID = direct:req_deployment_id", 
		"NFV_CATALOG_GET_NSD_BY_ID = direct:req_nsd_id", 
		"uri.to   = mock:output" })
@ActiveProfiles("testing")
public class LCMIntegrationTest {
	private static final transient Log logger = LogFactory.getLog(LCMIntegrationTest.class.getName());

	@Autowired
	RepositoryService repositoryService;

	
	RoutesBuilder builder = new RouteBuilder() {
		@Override
		public void configure() {
			from("direct:get_mocked_order").bean(scmocked, "getOrderById");
			from("direct:get_mocked_spec").bean(scmocked, "getSpecById");
			from("direct:get_mocked_add_service").bean(scmocked, "getMockedAddService");
			from("direct:get_mocked_upd_service").bean(scmocked, "getMockedService");
			from("direct:get_mocked_upd_order").bean(scmocked, "updateServiceOrder");
			from("direct:get_mocked_service_id").bean(scmocked, "getServiceById");
			from("direct:get_mocked_service_queueitems").bean(scmocked, "getServiceQueueItems");
			from("direct:get_mocked_lcmrulebyid").bean(scmocked, "getLCMRulebyID");
			from("direct:get_mocked_lcmrulesbyspecid").bean(scmocked, "getLCMRulesbySpecIDPhase(${header.servicespecid}, ${header.phasename})");
			from("direct:req_deploy_nsd").bean(scmocked, "req_deploy_nsd");
			from("direct:req_deployment_id").bean(scmocked, "req_deployment_id");
			from("direct:req_nsd_id").bean(scmocked, "req_nsd_id");

		};
	};



	SCMocked scmocked = new SCMocked();

	public ServiceCreate setupServiceCreate(ServiceSpecification aServiceSpec ) throws IOException {

		ServiceCreate aService;
		
		
		aService = new ServiceCreate();
		aService.setServiceCharacteristic( new ArrayList<>() );
        for (ServiceSpecCharacteristic sourceCharacteristic : aServiceSpec.getServiceSpecCharacteristic()) {
        	Characteristic newChar = new Characteristic();
			newChar.setName( sourceCharacteristic.getName() );
			newChar.setValueType( sourceCharacteristic.getValueType() );
			
			if ( sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.ARRAY.getValue() ) ||
					 sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.SET.getValue() ) ) {
				String valString = "";
				for (ServiceSpecCharacteristicValue specchar : sourceCharacteristic.getServiceSpecCharacteristicValue()) {
					if ( ( specchar.isIsDefault()!= null) && specchar.isIsDefault() ) {
						if ( !valString.equals("")) {
							valString = valString + ",";
						}
						valString = valString + "{\"value\":\"" + specchar.getValue().getValue() + "\",\"alias\":\"" + specchar.getValue().getAlias() + "\"}";
					}
					
				}				
				newChar.setValue( new Any( "[" + valString + "]", "") );
			} else {
				for (ServiceSpecCharacteristicValue specchar : sourceCharacteristic.getServiceSpecCharacteristicValue()) {
					if ( ( specchar.isIsDefault()!= null) && specchar.isIsDefault() ) {
						newChar.setValue( new Any(
								specchar.getValue().getValue(), 
								specchar.getValue().getAlias()) );
						break;
					}
				}						
			}
			
			
			if ( newChar.getValue() !=null) {
				aService.getServiceCharacteristic().add(newChar );
			} else {
				newChar.setValue( new Any(
						"", 
						"") );
				aService.getServiceCharacteristic().add(newChar );
			}
        }
        
        return aService;
	}
	
	
	
	@Test
	public void testLcmBaseExecutorAPIs() throws Exception {

		logger.debug("===============TEST START testLcmBaseExecutorAPIs =============================");
		/**
		 * Tests to perform and enhance API
		 * - Functions for ValueTypes (enum, Set, Arrays, numbers, etc)
		 * 		-String, Integer OK
		 * - Evaluate parameter values inside a json OK
		 * - Have variables: String, Integer OK
		 * - Operation for new Service Order (+ include sepc with its parameters..)
		 * - Operations for REST services
		 *
		 */
		
		
		
		class LcmBaseExecutorC extends LcmBaseExecutor {
			@Override
			public void exec() {				
			}

			

					
		}
		

		String sspectex = scmocked.getSpecById("f2b74f90-4140-4895-80d1-ef243398117b");		
		ServiceSpecification aServiceSpec = SCMocked.toJsonObj( sspectex, ServiceSpecification.class);	
		ServiceCreate aService = setupServiceCreate(aServiceSpec);	
		
		LcmBaseExecutorC be = new LcmBaseExecutorC();
		LCMRuleSpecification lcmspec = scmocked.getLCMRulebyIDJson("40f027b5-24a9-4db7-b422-a963c9feeb7a"); 
		be.setLcmspec( lcmspec );
        LCMRulesExecutorVariables vars = new LCMRulesExecutorVariables(aServiceSpec, new ServiceOrder(), null, aService, null, null);
		be.setVars( vars );
		assertThat( be.getCharValFromStringType("cirros_2vnf_ns::SSHKEY") ).isEqualTo("MYKEYX");
		assertThat( be.getCharValAsString("cirros_2vnf_ns::SSHKEY") ).isEqualTo("MYKEYX");
		assertThat( be.getCharValAsString("Quality Class") ).isEqualTo("0");
		assertThat( be.getCharValFromBooleanType("High Availability") ).isEqualTo(false);
		
		
		sspectex = scmocked.getSpecById("0d5551e6-069f-43b7-aa71-10530f290239");
		aServiceSpec = SCMocked.toJsonObj( sspectex, ServiceSpecification.class);	
		aService = setupServiceCreate(aServiceSpec);	
		be = new LcmBaseExecutorC();
		be.setLcmspec( lcmspec );
		vars = new LCMRulesExecutorVariables(aServiceSpec, new ServiceOrder(), null, aService, null, null);
		be.setVars( vars );
		
		be.setCharValFromBooleanType("High Availability", true);
		assertThat( be.getCharValFromBooleanType("High Availability") ).isEqualTo(true);
		assertThat( be.checkIfSetContainsValue(be.getCharValFromSetType("Area of Service"), "GR") ).isEqualTo(true);
		assertThat( be.checkIfSetContainsValue(be.getCharValFromSetType("Area of Service"), "ES") ).isEqualTo(true);
		assertThat( be.checkIfSetContainsValue(be.getCharValFromSetType("Area of Service"), "XXX") ).isEqualTo(false);
		 
		be.setCharValFromSetType("cirros_2vnf_nsd::Primitive::fsetup", "[{\"value\":\"1\",\"alias\":\"member_vnf_index\"},{\"value\":\"fsetup\",\"alias\":\"primitive\"},{\"value\":\"{      \\\"tvg\\\": {         \\\"ip\\\": \\\"\\\",         \\\"channel1\\\": {             \\\"mode\\\": \\\"0\\\"         }     } }\",\"alias\":\"confjson\"}]");
		
		assertThat( be.checkIfSetContainsValue(be.getCharValFromSetType("cirros_2vnf_nsd::Primitive::fsetup"), "fsetup") ).isEqualTo(true);
		
		be.setCharValFromSetType("cirros_2vnf_nsd::Primitive::fsetup", "[{\"value\":\"1\",\"alias\":\"member_vnf_index\"},{\"value\":\"fsetupchanged\",\"alias\":\"primitive\"},{\"value\":\"{      \\\"tvg\\\": {         \\\"ip\\\": \\\"\\\",         \\\"channel1\\\": {             \\\"mode\\\": \\\"0\\\"         }     } }\",\"alias\":\"confjson\"}]");
		assertThat( be.checkIfSetContainsValue(be.getCharValFromSetType("cirros_2vnf_nsd::Primitive::fsetup"), "fsetupchanged") ).isEqualTo(true);
		

		logger.debug("===============TEST END testLcmBaseExecutorAPIs =============================");
	}
	
	@Test
	public void testExecRuleSpec() throws Exception {

		logger.debug("===============TEST START testExecRuleSpec =============================");
		
		//TestExSpec3.json
		String sspectex = scmocked.getSpecById("0d5551e6-069f-43b7-aa71-10530f290239");		
		ServiceSpecification aServiceSpec = SCMocked.toJsonObj( sspectex, ServiceSpecification.class);		
		ServiceCreate aService = setupServiceCreate(aServiceSpec);
		
        LCMRulesExecutorVariables vars = new LCMRulesExecutorVariables(aServiceSpec, new ServiceOrder(), null, aService, null, null);
		LCMRulesExecutor lcmRulesExecutor = new LCMRulesExecutor();
		//check LcmCirrosRule3Test code for error (more complex code)

        logger.debug( "BEFORE vars.getServiceToCreate() = " + vars.getServiceToCreate().toString() );
		LCMRuleSpecification lcs = scmocked.getLCMRulebyIDJson("8b7b8339-0c33-4731-af9c-c98adadbe777"); 
	    vars = lcmRulesExecutor.executeLCMRuleCode(  lcs, vars);
	    assertThat( vars.getCompileDiagnosticErrors().size() ).isEqualTo(0);
        logger.debug( "AFTER EXEC vars.getServiceToCreate() = " + vars.getServiceToCreate().toString() );
	    
	    //TestExBundleSpec.json
		sspectex = scmocked.getSpecById("f2b74f90-4140-4895-80d1-ef243398117b");		
		aServiceSpec = SCMocked.toJsonObj( sspectex, ServiceSpecification.class);	
		aService = setupServiceCreate(aServiceSpec);	
  	
		lcs = scmocked.getLCMRulebyIDJson("40f027b5-24a9-4db7-b422-a963c9feeb7a");
		vars = new LCMRulesExecutorVariables(aServiceSpec, new ServiceOrder(), null, aService, null, null);
	    vars = lcmRulesExecutor.executeLCMRuleCode(  lcs, vars);
	    assertThat( vars.getCompileDiagnosticErrors().size() ).isEqualTo(0);
	    
		assertThat(
				vars.getServiceToCreate().getServiceCharacteristic()
					.stream()
					.filter(c -> c.getName().equals("cirros_2vnf_ns::OSM_CONFIG"))
					.findFirst().get().getValue().getValue()
				).contains("cccccccc-8219-4580-9697-bf4a8f0a08f9");
		

		lcs = scmocked.getLCMRulebyIDJson("75cebf16-1699-486f-8304-d6512f90c910");
	    vars = lcmRulesExecutor.executeLCMRuleCode(  lcs, vars);


		assertThat(
				vars.getServiceToCreate().getServiceCharacteristic()
					.stream()
					.filter(c -> c.getName().equals("cirros_2vnf_ns::OSM_CONFIG"))
					.findFirst().get().getValue().getValue()
				).contains("cccccccc-8219-4580-9697-bf4a8f0a08f9");
		assertThat(
				vars.getServiceToCreate().getServiceCharacteristic()
					.stream()
					.filter(c -> c.getName().equals("cirros_2vnf_ns::SSHKEY"))
					.findFirst().get().getValue().getValue()
				).isEqualTo("MYKEYXExampleConcatSSHKEY_EnhancedByRule");
		

		logger.debug("Will make web calls. This is made online.");

//		lcs = scmocked.getLCMRulebyIDJson("49e2e679-9dc1-4c7b-abd9-72377d4c1a5d"); 
//	    vars = lcmRulesExecutor.executeLCMRuleCode(  lcs, vars);//this includes a post	    
	    
		lcs = scmocked.getLCMRulebyIDJson("c1bd362d-011f-485b-a7d9-3bb05a2f6868"); 
	    vars = lcmRulesExecutor.executeLCMRuleCode(  lcs, vars);// this includes a GET and payload json to service

		logger.debug("===============TEST END testExecRuleSpec =============================");
	}
	

	
}
