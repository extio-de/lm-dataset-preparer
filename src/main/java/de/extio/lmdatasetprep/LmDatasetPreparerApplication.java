package de.extio.lmdatasetprep;

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

@SpringBootApplication
@EnableCaching
public class LmDatasetPreparerApplication implements CommandLineRunner, ApplicationContextAware {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LmDatasetPreparerApplication.class);
	
	public static void main(final String[] args) {
		SpringApplication.run(LmDatasetPreparerApplication.class, args);
	}
	
	private ApplicationContext applicationContext;
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(final String... args) throws Exception {
		if (args.length == 0) {
			LOGGER.error("Commandline argument is missing: Provide the tool name as argument");
			return;
		}
		
		final var bean = this.applicationContext.getBean(args[0]);
		if (!(bean instanceof Consumer)) {
			LOGGER.info("Invalid bean {}", args[0]);
			return;
		}
		
		LOGGER.info("Executing {}", args[0]);
		try {
			((Consumer<String[]>) bean).accept(args);
		}
		catch (Exception e) {
			LOGGER.error("Error executing the component", e);
		}
	}
	
	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
}
