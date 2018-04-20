package controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import controllers.BaseController;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class HomeControllerTest {

	@InjectMocks
	private BaseController homeController;

	private MockMvc mockMvc;
	
	String formattedDate;

	@Before
	public void setup() {
		Locale locale = new Locale("en");
		Date date = new Date();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);

		formattedDate = dateFormat.format(date);
		// Process mock annotations
		MockitoAnnotations.initMocks(this);
		
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/view/");
        viewResolver.setSuffix(".jsp");
		this.mockMvc = MockMvcBuilders.standaloneSetup(homeController)
				.setViewResolvers(viewResolver)
				.build();
		// Setup Spring test in standalone mode
		//this.mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

	}

	@Test
    public void testAbout() throws Exception {
        this.mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(forwardedUrl("/WEB-INF/view/home.jsp"))
                .andExpect(model().attributeExists("serverTime"));
     
    }
	
	@Test
    public void testlogin() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/login").param("error", "e1").param("logout", "msg1"))
        		.andExpect(status().isOk())
        		.andExpect(view().name("login"))
                .andExpect(forwardedUrl("/WEB-INF/view/login.jsp"));
                //.andExpect(model().equals(obj));
        		
    }
}