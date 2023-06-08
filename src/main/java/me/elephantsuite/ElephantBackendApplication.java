package me.elephantsuite;

import me.elephantsuite.config.PropertiesHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class ElephantBackendApplication {

	public static Logger LOGGER = LogManager.getLogger(ElephantBackendApplication.class);

	public static final PropertiesHandler ELEPHANT_CONFIG = PropertiesHandler
		.builder()
		.addConfigOption("isDevelopment", true)
		.addConfigOption("senderEmailAddress", "no-reply@elephantsuite.net")
		.addConfigOption("elephantDomain", "localhost:8080")
		.addConfigOption("pfpIdMax", 47)
		.addConfigOption("recentlyViewedDecksMax", 50)
		.addConfigOption("tokenExpiredLimitMinutes", 15)
		.setFileName("elephant-config.properties")
		.addConfigOption("welcomeMessageHtmlFile", "WelcomeRedirectMessage.html")
		.addConfigOption("confirmationEmailHtmlFile", "ConfirmAccountEmail.html")
		.addConfigOption("forgotPasswordEmailHtmlFile", "ForgotPasswordEmail.html")
		.addConfigOption("friendEmailHtmlFile", "FriendEmailHtmlFile.html")
		.addConfigOption("inviteEmailHtmlFile", "InviteEmailHtmlFile.html")
		.build();

	public static void main(String[] args) {
		SpringApplication.run(ElephantBackendApplication.class, args);
	}


	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry
					.addMapping("/**")
					.allowedOrigins("*")
					.allowedMethods("*");
			}
		};
	}
}
