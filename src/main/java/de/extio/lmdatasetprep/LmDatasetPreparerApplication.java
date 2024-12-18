package de.extio.lmdatasetprep;

import java.util.Arrays;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;

import de.extio.lmdatasetprep.preparer.DatasetTool;

@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = { "de.extio.lmdatasetprep", "de.extio.lmlib" })
public class LmDatasetPreparerApplication implements CommandLineRunner, ApplicationContextAware {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LmDatasetPreparerApplication.class);
	
	public static void main(final String[] args) {
		SpringApplication.run(LmDatasetPreparerApplication.class, args);
	}
	
	public static ApplicationContext applicationContext;
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(final String... args) throws Exception {
		if (args.length == 0) {
			LOGGER.error("Commandline argument is missing: Provide the tool name as argument");
			this.logAvailableTools();
			return;
		}
		
		final var bean = applicationContext.getBean(args[0]);
		if (!(bean instanceof Consumer) || !(bean instanceof DatasetTool)) {
			LOGGER.error("Invalid bean {}", args[0]);
			this.logAvailableTools();
			return;
		}
		
		LOGGER.info("Executing {}", args[0]);
		try {
			((Consumer<String[]>) bean).accept(args);
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
