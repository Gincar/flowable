package com.chiyew;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Gincar
 * date 2024/6/19 20:38
 */
public class HolidayRequest {
    public static void main(String[] args){
        /*创建了一个独立(standalone)配置对象。这里的'独立'指的是引擎是完全独立创建及使用的（而不是在Spring环境中使用，
        这时需要使用SpringProcessEngineConfiguration类代替）*/
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                /*内存H2数据库实例的JDBC连接参数，数据库在JVM重启后会消失。如果需要永久保存数据，需要切换为持久化数据库，并相应切换连接参数*/
                .setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")
                /*确保在JDBC参数连接的数据库中，数据库表结构不存在时，会创建相应的表结构*/
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = cfg.buildProcessEngine();

        /*将流程定义部署至Flowable引擎*/
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();


        /*查询已经部署在引擎中的流程定义*/
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        System.out.println("已经定义的流程是 : " + processDefinition.getName());

        /*模拟用户输入请假信息*/
        Scanner scanner= new Scanner(System.in);

        System.out.println("请假人姓名?");
        String employee = scanner.nextLine();

        System.out.println("请假天数?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());

        System.out.println("请假原因?");
        String description = scanner.nextLine();

        /*在流程实例启动后，会创建一个执行(execution)，并将其放在启动事件上。从这里开始，这个执行沿着顺序流移动到经理审批的用户任务，
        并执行用户任务行为。这个行为将在数据库中创建一个任务，该任务可以之后使用查询找到。用户任务是一个等待状态(wait state)，
        引擎会停止执行，返回API调用处。*/
        RuntimeService runtimeService = processEngine.getRuntimeService();

        Map<String, Object> variables = new HashMap<>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("holidayRequest", variables);

        /*要获得实际的任务列表，需要通过TaskService创建一个TaskQuery。我们配置这个查询只返回’managers’组的任务：*/
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("你有 " + tasks.size() + " 个任务:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }

        /*可以使用任务Id获取特定流程实例的变量，并在屏幕上显示实际的申请：*/
        System.out.println("你想办结哪个任务?");
        int taskIndex = Integer.parseInt(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " 想要申请 " +
                processVariables.get("nrOfHolidays") + " 天假，是否允许批准?（批准：Y/驳回：N）");

        /*经理现在就可以完成任务了。在现实中，这通常意味着由用户提交一个表单。表单中的数据作为流程变量传递。
        在这里，我们在完成任务时传递带有’approved’变量（这个名字很重要，因为之后会在顺序流的条件中使用！）的map来模拟：*/
        boolean approved = "y".equalsIgnoreCase(scanner.nextLine());
        variables = new HashMap<>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);

        /*现在任务完成，并会在离开排他网关的两条路径中，基于’approved’流程变量选择一条。*/

        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();

        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " 花费了 "
                    + activity.getDurationInMillis() + " 毫秒");
        }

    }
}
