package io.openslice.osom;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@ComponentScan
public class OsomSpringBoot implements CommandLineRunner {

	private static ApplicationContext applicationContext;

	@Autowired
	RepositoryService repositoryService;

	@Autowired
	RuntimeService runtimeService;

	@Autowired
	TaskService taskService;

	@Override
	public void run(String... arg0) throws Exception {
		if (arg0.length > 0 && arg0[0].equals("exitcode")) {
			throw new ExitException();
		}

		System.out
				.println("Number of process definitions : " + repositoryService.createProcessDefinitionQuery().count());
		System.out.println("Number of tasks : " + taskService.createTaskQuery().count());
//		runtimeService.startProcessInstanceByKey("oneTaskProcess");
//		System.out.println("Number of tasks after process start: " + taskService.createTaskQuery().count());
	}

	public static void main(String[] args) throws Exception {

		applicationContext = new SpringApplication(OsomSpringBoot.class).run(args);

//        for (String beanName : applicationContext.getBeanDefinitionNames()) {
//            System.out.println(beanName);
//        }
	}

	class ExitException extends RuntimeException implements ExitCodeGenerator {
		private static final long serialVersionUID = 1L;

		@Override
		public int getExitCode() {
			return 10;
		}

	}

}