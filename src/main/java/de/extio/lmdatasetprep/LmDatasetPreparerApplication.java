package de.extio.lmdatasetprep;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import de.extio.lmdatasetprep.tools.DatasetTool;

@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = { "de.extio.lmdatasetprep", "de.extio.lmlib" })
public class LmDatasetPreparerApplication implements CommandLineRunner, ApplicationContextAware {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LmDatasetPreparerApplication.class);
	
	public static void main(final String[] args) {
		SpringApplication.run(LmDatasetPreparerApplication.class, args);
	}
	
	public static ApplicationContext applicationContext;
	
	@Override
	public void run(final String... args) throws Exception {
		if (args.length < 2) {
			LOGGER.error("Commandline arguments are invalid. Usage: <tool properties file> [ <tool 1>, ... ]");
			this.logAvailableTools();
			return;
		}
		
		LOGGER.info("Loading tools");
		final List<DatasetTool> tools = new ArrayList<>(args.length - 1);
		for (int i = 1; i < args.length; i++) {
			try {
				tools.add(applicationContext.getBean(args[i], DatasetTool.class));
			}
			catch (NoSuchBeanDefinitionException | BeanNotOfRequiredTypeException e) {
				LOGGER.error("Invalid bean {}", args[i]);
				this.logAvailableTools();
				return;
			}
		}
		
		LOGGER.info("Loading tool properties");
		final Properties toolProps = new Properties();
		toolProps.load(Files.newInputStream(Path.of(args[0])));
		
		try {
			LOGGER.info("Executing {}", tools.stream().map(tool -> tool.getClass().getSimpleName()).collect(Collectors.joining(", ")));
			final List<Thread> threads = new ArrayList<>(tools.size());
			for (final var tool : tools) {
				final Thread t = new Thread(() -> tool.accept(toolProps));
				t.setName(StringUtils.abbreviateMiddle(tool.getClass().getSimpleName(), ".", 15));
				t.start();
			}
			
			int cnt = 0;
			while (cnt < 5) {
				Thread.sleep(1000);
				if (Execution.startedFileConsumers.get() == 0 || Execution.startedFileConsumers.get() > Execution.finishedFileConsumers.get() ||
						Execution.tasksRunning.get() != 0 || Execution.work.values().stream().anyMatch(q -> q.values().stream().anyMatch(bq -> !bq.isEmpty()))) {
					
					cnt = 0;
				}
				else {
					cnt++;
				}
			}
			LOGGER.info("All file consumers are finished, All queues are empty");
			
			Execution.shutdown.set(true);
			for (final var thread : threads) {
				thread.join();
			}
			
			LOGGER.info("Shutdown");
			Execution.executorService.shutdown();
			((ConfigurableApplicationContext) applicationContext).close();
		}
		catch (final Exception e) {
			LOGGER.error("Error executing the component", e);
		}
	}
	
	private void logAvailableTools() {
		LOGGER.error("Available tools: {}", Arrays.toString(applicationContext.getBeanNamesForType(DatasetTool.class)));
	}
	
	@Override
	public void setApplicationContext(final ApplicationContext appContext) throws BeansException {
		applicationContext = appContext;
	}
	
}
