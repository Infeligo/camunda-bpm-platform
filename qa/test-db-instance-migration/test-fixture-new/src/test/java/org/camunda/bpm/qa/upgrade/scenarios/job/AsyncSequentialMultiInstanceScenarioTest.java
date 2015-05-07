package org.camunda.bpm.qa.upgrade.scenarios.job;

import static org.camunda.bpm.qa.upgrade.util.ActivityInstanceAssert.assertThat;
import static org.camunda.bpm.qa.upgrade.util.ActivityInstanceAssert.describeActivityInstanceTree;

import org.camunda.bpm.engine.management.JobDefinition;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.qa.upgrade.ScenarioUnderTest;
import org.camunda.bpm.qa.upgrade.UpgradeTestRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

@ScenarioUnderTest("AsyncSequentialMultiInstanceSubprocessScenario")
public class AsyncSequentialMultiInstanceScenarioTest {

  @Rule
  public UpgradeTestRule rule = new UpgradeTestRule();

  @Test
  @ScenarioUnderTest("initAsyncBefore.1")
  public void testInitAsyncBeforeCompletionCase1() {
    // given
    Job asyncJob = rule.jobQuery().singleResult();

    // when
    rule.getManagementService().executeJob(asyncJob.getId());

    // then the process can be completed successfully
    for (int i = 0; i < 3; i++) {
      Task subProcessTask = rule.taskQuery().singleResult();
      Assert.assertNotNull(subProcessTask);
      rule.getTaskService().complete(subProcessTask.getId());
    }

    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("initAsyncBefore.2")
  public void testInitAsyncBeforeActivityInstanceTree() {
    // given
    ProcessInstance instance = rule.processInstance();

    // when
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(instance.getId());

    // then
    Assert.assertNotNull(activityInstance);
    assertThat(activityInstance).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
          // this is not the multi-instance body because the execution
          // references the inner activity
          .transition("miSubProcess")
        .done());
  }

  @Test
  @ScenarioUnderTest("initAsyncBefore.3")
  public void testInitAsyncBeforeDeletion() {
    // given
    ProcessInstance instance = rule.processInstance();

    // when
    rule.getRuntimeService().deleteProcessInstance(instance.getId(), null);

    // then
    rule.assertScenarioEnded();
  }

  /**
   * Note: this test is not really isolated since the job
   * definition is migrated when the process definition is accessed the first time.
   * This might happen already before this test case is executed.
   */
  @Test
  @ScenarioUnderTest("initAsyncBefore.4")
  public void testInitAsyncBeforeJobDefinition() {
    // when the process is redeployed into the cache (instantiation should trigger that)
    rule.getRuntimeService().startProcessInstanceByKey("AsyncBeforeSequentialMultiInstanceSubprocess");

    // then the old job definition referencing "miSubProcess" has been migrated
    JobDefinition asyncJobDefinition = rule.jobDefinitionQuery().singleResult();
    Assert.assertEquals("miSubProcess#multiInstanceBody", asyncJobDefinition.getActivityId());
  }

}