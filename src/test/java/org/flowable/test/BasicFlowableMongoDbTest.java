/* Licensed under the Apache License, Version 2.0 (the "License");
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
 */
package org.flowable.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.test.delegate.StartProcessInstanceDelegate;
import org.flowable.test.delegate.ThrowsExceptionTestJavaDelegate;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class BasicFlowableMongoDbTest extends AbstractMongoDbTest {
    
    @Test
    public void testDeployProcess() throws Exception {
        String testDeploymentId = repositoryService.createDeployment().addClasspathResource("oneTaskProcess.bpmn20.xml")
            .deploy()
            .getId();
        assertEquals(1, repositoryService.createDeploymentQuery().count());
        assertEquals(1, repositoryService.createDeploymentQuery().list().size());
        
        Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertNotNull(deployment);
        assertNotNull(deployment.getId());
        assertNotNull(deployment.getDeploymentTime());
        
        assertEquals(1, repositoryService.createProcessDefinitionQuery().count());
        assertEquals(1, repositoryService.createProcessDefinitionQuery().list().size());
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertNotNull(processDefinition.getId());
        assertEquals(deployment.getId(), processDefinition.getDeploymentId());
        assertEquals("oneTask", processDefinition.getKey());
        assertEquals(1, processDefinition.getVersion());

        InputStream resourceStream = repositoryService.getResourceAsStream(testDeploymentId, "oneTaskProcess.bpmn20.xml");
        String xml = new String(IoUtil.readInputStream(resourceStream, "oneTaskProcess.bpmn20.xml"), "UTF-8");
        assertTrue(xml.length() > 0);
    }

    @Test
    public void testRedeploy() {
        repositoryService.createDeployment().addClasspathResource("oneTaskProcess.bpmn20.xml").deploy();
        assertEquals(1, repositoryService.createProcessDefinitionQuery().count());
        assertEquals(1, repositoryService.createProcessDefinitionQuery().list().size());

        for (int i = 0; i < 8; i++) {
            repositoryService.createDeployment().addClasspathResource("oneTaskProcess.bpmn20.xml").deploy();
        }
        assertEquals(9, repositoryService.createProcessDefinitionQuery().count());
        assertEquals(9, repositoryService.createProcessDefinitionQuery().list().size());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("oneTask")
            .latestVersion()
            .singleResult();
        assertEquals(9, processDefinition.getVersion());

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("oneTask")
            .orderByProcessDefinitionVersion().asc()
            .list();
        for (int i = 0; i < 9; i++) {
            assertEquals(i + 1, processDefinitions.get(i).getVersion());
        }
    }
    
    @Test
    public void testOneTaskProcess() {
        repositoryService.createDeployment().addClasspathResource("oneTaskProcess.bpmn20.xml").deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");
        assertNotNull(processInstance.getId());
        assertNotNull(processInstance.getProcessDefinitionKey());
        assertNotNull(processInstance.getProcessDefinitionId());
        assertNotNull(processInstance.getProcessDefinitionVersion());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());
        assertEquals(2, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        ProcessInstance processInstanceResult = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstanceResult);
        assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list().size());

        Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        assertNotNull(task.getId());
        assertEquals("my task", task.getName());
        assertEquals(processInstance.getId(), task.getProcessInstanceId());
        assertEquals(task.getProcessDefinitionId(),processInstance.getProcessDefinitionId());
        
        taskService.complete(task.getId());
        
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }
    
    @Test
    public void testJavaDelegate() {
        repositoryService.createDeployment().addClasspathResource("javadelegate.bpmn20.xml").deploy();
        
        ThrowsExceptionTestJavaDelegate.FAIL = false;
        runtimeService.startProcessInstanceByKey("delegateProcess");
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
    }
    
    @Test
    public void testJavaDelegateExceptionCausesRollback() {
        repositoryService.createDeployment().addClasspathResource("javadelegate.bpmn20.xml").deploy();
        
        ThrowsExceptionTestJavaDelegate.FAIL = true;
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
        try {
            runtimeService.startProcessInstanceByKey("delegateProcess");
            fail("Should throw an exception");
        } catch (Exception e) { }
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }
    
    @Test
    public void testBasicVariables() {
        repositoryService.createDeployment().addClasspathResource("oneTaskProcess.bpmn20.xml").deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");
        runtimeService.setVariable(processInstance.getId(), "simpleText", "test");
        runtimeService.setVariable(processInstance.getId(), "simpleNumber", 10);
        
        Map<String, Object> variableMap = runtimeService.getVariables(processInstance.getId());
        assertEquals("test", variableMap.get("simpleText"));
        assertEquals(10, variableMap.get("simpleNumber"));
    }

    @Test
    public void testBasicExclusiveGateway() {
        repositoryService.createDeployment().addClasspathResource("basicExclusiveGatewayProcess.bpmn20.xml").deploy();
        
        runtimeService.startProcessInstanceByKey("gateway", Collections.singletonMap("gotoFirstTask", true));
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("my task", task.getName());
        taskService.complete(task.getId());
        
        runtimeService.startProcessInstanceByKey("gateway", Collections.singletonMap("gotoFirstTask", false));
        task = taskService.createTaskQuery().singleResult();
        assertEquals("my task2", task.getName());
        taskService.complete(task.getId());
    }
    
    @Test
    public void testAsyncServiceTask() {
        repositoryService.createDeployment().addClasspathResource("async.bpmn20.xml").deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncProcess");
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        
        managementService.executeJob(job.getId());
        
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("theUserTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    public void testNestedTransactions() {
        ThrowsExceptionTestJavaDelegate.COUNT = new AtomicInteger(0);
        ThrowsExceptionTestJavaDelegate.FAIL = true;

        // Starts a process instance in a service task through the runtime service.
        // That second process instance will throw an exception and everything should roll back,
        // as the MongoDbSession should be reused.
        repositoryService.createDeployment()
            .addClasspathResource("nestedtransaction.bpmn20.xml")
            .addClasspathResource("throwsException.bpmn20.xml")
            .deploy();
        try {
            runtimeService.startProcessInstanceByKey("nestedTransaction");
            fail("Expected exception");
        } catch(Exception e) {
            e.printStackTrace();
        }
        assertEquals(1, StartProcessInstanceDelegate.COUNT.get());
        assertEquals(1, ThrowsExceptionTestJavaDelegate.COUNT.get());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

}
