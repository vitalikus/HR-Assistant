package ch.belsoft.hrassistant.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.RandomStringUtils;

import ch.belsoft.hrassistant.config.controller.ConfigurationController;
import ch.belsoft.hrassistant.config.dao.ConfigurationDAO;
import ch.belsoft.hrassistant.config.model.ConfigDefault;
import ch.belsoft.hrassistant.config.model.ConfigParam;
import ch.belsoft.hrassistant.config.model.ConfigParams;
import ch.belsoft.hrassistant.config.model.ConfigParamsMenuCategory;
import ch.belsoft.hrassistant.config.model.ConfigType;
import ch.belsoft.hrassistant.config.model.IConfiguration;
import ch.belsoft.hrassistant.model.User;
import ch.belsoft.tools.Logging;
import ch.belsoft.tools.Util;
import ch.belsoft.tools.XPagesUtil;

public class ApplicationController implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String REALHOMEPAGE = "hr_dashboard.xsp";
	private static final String REALJOBLISTING = "appl_joblisting.xsp";
	private static final String REALLOGIN = "login.xsp";
	private static final String BEAN_CONFIGURATIONCONTROLLER = "configurationController";
	private static final String BEAN_CONFIGURATIONDAO = "configurationDAO";
	private static final String BEAN_NAME = "applicationController";

	private final LinkedHashMap<ConfigType, List<String>> configSelections = new LinkedHashMap<ConfigType, List<String>>();
	private final LinkedHashMap<ConfigType, LinkedHashMap<String, ConfigDefault>> configMap = new LinkedHashMap<ConfigType, LinkedHashMap<String, ConfigDefault>>();
	private final LinkedHashMap<ConfigParamsMenuCategory, List<ConfigDefault>> configMapMenuItems = new LinkedHashMap<ConfigParamsMenuCategory, List<ConfigDefault>>();

	/**
	 * Setting can be setted in order to meet a "parent" page name and so the
	 * menu item will be highlighted..
	 */
	private String currentPageNameParent = "";
	private String currentPage = "";
	private String currentQueryString = "";

	private ConfigurationController configurationController = null;
	private User user = new User();

	public ApplicationController() {
		initConfiguration();
	}

	public static ApplicationController get() {
		return (ApplicationController) XPagesUtil.resolveVariable(BEAN_NAME);
	}

	public String getLinkEditMode() {
		return XPagesUtil.getCurrentUrl().replace("readonly=true",
				"readonly=false");
	}

	public String getUsername() {
		return XPagesUtil.getUserNameCommon();
	}

	/**
	 * getting the XSP Contect and redirect
	 */
	public void redirectToRealHomePage() {
		try {
			XPagesUtil.getXSPContext().redirectToPage(REALHOMEPAGE, true);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void clearConfiguration() {
		this.configSelections.clear();
		this.configMap.clear();
		this.configMapMenuItems.clear();
	}

	public void initConfiguration() {
		try {

			this.clearConfiguration();
			this.configurationController = (ConfigurationController) XPagesUtil
					.getViewScope().get(BEAN_CONFIGURATIONCONTROLLER);
			if (this.configurationController == null) {

				this.configurationController = new ConfigurationController();

				// FacesContext context = FacesContext.getCurrentInstance();
				// ConfigurationDAO configurationDAO = (ConfigurationDAO)
				// context
				// .getApplication().createValueBinding(
				// "#{configurationDAO}").getValue(context);
				ConfigurationDAO configurationDAO = (ConfigurationDAO) XPagesUtil
						.getBindingValue(XPagesUtil
								.getBindingValueName(BEAN_CONFIGURATIONDAO));

				this.configurationController
						.setConfigurationDAO(configurationDAO);
				this.configurationController.setApplicationController(this);

				XPagesUtil.getViewScope().put(BEAN_CONFIGURATIONCONTROLLER,
						this.configurationController);

			}

			for (ConfigDefault config : ConfigurationController.get()
					.getConfigurations()) {
				this.addConfig(config);
			}

		} catch (Exception e) {
			// Logging.logError(e);
		}
	}

	public void authanticateUser() {
		currentPage = XPagesUtil.getViewRoot().getPageName();
		currentQueryString = XPagesUtil.getXSPContext().getUrl().getQueryString();
		String hostName = XPagesUtil.getHostName();
		// to avoid that we get stupid to login all the time, when we develop
		// ;-)
		
		if (!hostName.contains("localhost") && !hostName.contains("belsoft")) {
			if (!user.isAuthenticated()) {
				
				generatePassword();

				if (!"/login.xsp".equals(currentPage)
						&& !"/error.xsp".equals(currentPage)) {
					redirectToLoginPage();
				}
			}
		} else {
			user.setUserName("Frank&Fred");
		}
	}

	public void loginUser() {
		user.setAuthenticated(true);
		redirectBasedOnRole();
	}

	public void redirectBasedOnRole() {
		if (user.getRole().equals(
				ConfigParamsMenuCategory.JOB_APPLICANT.toString())) {
			redirectToJobListingPage();
		} else if (user.getRole().equals(
				ConfigParamsMenuCategory.HR.toString()) && currentPage.contains(REALJOBLISTING)) {
				redirectToRealHomePage();
		} else if(currentPage.contains(REALLOGIN)){
			redirectToRealHomePage();
		}
		
		XPagesUtil.getXSPContext().redirectToPage(currentPage+currentQueryString, true);
	}

	public void logoutUser() {
		user = new User();
		authanticateUser();
	}

	private void generatePassword() {
		String randomString = RandomStringUtils.random(5, true, true);
		user.setPasswordGenerated(randomString);
	}

	public void redirectToLoginPage() {
		try {
			XPagesUtil.getXSPContext().redirectToPage(REALLOGIN, false);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void redirectToJobListingPage() {
		try {
			XPagesUtil.getXSPContext().redirectToPage(REALJOBLISTING, true);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public List<ConfigDefault> getMenuItems() {

		List<ConfigDefault> result = new ArrayList<ConfigDefault>();
		try {
			LinkedHashMap<String, ConfigDefault> map = this.configMap
					.get(ConfigType.MENU_ITEM);
			// result.add(map.entrySet());
			result = new ArrayList<ConfigDefault>(map.values());
		} catch (Exception e) {
			// Logging.logError(e);
		}
		return result;
	}

	public String getCurrentPageName() {
		return XPagesUtil.getCurrentPageName();
	}

	public IConfiguration getConfig(ConfigType configType, String sKey) {
		IConfiguration result = null;

		try {

			if (!configMap.containsKey(configType)) {
				initConfiguration();
				Util.logEvent("Configuration with type: " + configType
						+ " not found..");
			} else {
				LinkedHashMap<String, ConfigDefault> configByKeys = configMap
						.get(configType);
				if (!configByKeys.containsKey(sKey)) {
					Util.logEvent("Configuration in type " + configType
							+ " with key :" + sKey + " not found");
				} else {
					result = configByKeys.get(sKey);
					/*
					 * Util.logEvent("getConfig, value:  " + sType +
					 * " with key :" + sKey + " :::" + result + ", value:" +
					 * result.getValue());
					 */
				}
			}
		} catch (Exception e) {
			Logging.logError(e);
		}

		return result;
	}

	public String getConfigValue(ConfigType configType, String sKey) {
		String result = "";
		try {
			// Util.logEvent("inside getConfigValue: " + sType + ", key: " +
			// sKey);

			if (sKey != null && !sKey.equals("")) {
				IConfiguration config = getConfig(configType, sKey);

				if (config != null) {
					result = config.getConfigValue();
				} else {
					result = sKey;
				}
			}

		} catch (Exception e) {
			Logging.logError(e);
		}
		return result;
	}

	public List<IConfiguration> getConfigItems(String type) {
		List<IConfiguration> result = new ArrayList<IConfiguration>();

		try {

			ConfigType configType = ConfigType.valueOf(type);
			result = getConfigItems(configType);

		} catch (Exception e) {
			Logging.logError(e);
		}

		return result;
	}

	public List<IConfiguration> getConfigItems(ConfigType configType) {
		List<IConfiguration> result = new ArrayList<IConfiguration>();

		try {

			if (!configMap.containsKey(configType)) {
				initConfiguration();
			}

			result = new ArrayList<IConfiguration>(configMap.get(configType)
					.values());

		} catch (Exception e) {
			Logging.logError(e);
		}

		return result;
	}

	public List<String> getConfigSelection(ConfigType configType) {

		List<String> vResult = new Vector<String>();

		try {
			if (!configSelections.containsKey(configType)) {
				initConfiguration();
			}

			vResult = configSelections.get(configType);

		} catch (Exception e) {
			Logging.logError(e);
		}

		return vResult;
	}

	public List<ConfigParamsMenuCategory> getConfigMenuItemCategories() {
		List<ConfigParamsMenuCategory> result = new ArrayList<ConfigParamsMenuCategory>();

		try {
			List<ConfigParamsMenuCategory> categories = new ArrayList<ConfigParamsMenuCategory>(
					this.configMapMenuItems.keySet());

			if ("Administration".equals(user.getRole())) {
				// all the categories
				result.addAll(categories);
			} else {
				for (ConfigParamsMenuCategory category : categories) {
					if (category.toString().equals(user.getRole())) {
						result.add(category);
					}
				}
			}

		} catch (Exception e) {
			Logging.logError(e);
		}

		return result;
	}

	public List<ConfigDefault> getConfigMenuItemsByCategory(
			ConfigParamsMenuCategory menuCategory) {

		List<ConfigDefault> result = new ArrayList<ConfigDefault>();

		try {
			result = this.configMapMenuItems.get(menuCategory);
		} catch (Exception e) {
			Logging.logError(e);
		}

		return result;
	}

	private void addConfig(ConfigDefault configItem) {
		try {

			switch (configItem.getType()) {
			case MENU_ITEM:
				ConfigParam configParam = configItem
						.getParam(ConfigParams.MENU_CATEGORY);
				ConfigParamsMenuCategory menuCat = ConfigParamsMenuCategory
						.valueOf(configParam.getParamValue());
				if (!this.configMapMenuItems.containsKey(menuCat)) {
					this.configMapMenuItems.put(menuCat,
							new ArrayList<ConfigDefault>());
				}
				this.configMapMenuItems.get(menuCat).add(configItem);
				// configItem = new ConfigCredentials(configType, key, value);
				// ((ConfigCredentials) configItem).setPassword(arrParams[0]);
				break;
			default:
				// no default
				// configItem = new ConfigDefault(configType, key, value);
				break;
			}

			LinkedHashMap<String, ConfigDefault> mapConfig = null;
			List<String> vConfigSelection = null;

			if (configMap.containsKey(configItem.getType())) {
				mapConfig = configMap.get(configItem.getType());
			} else {
				mapConfig = new LinkedHashMap<String, ConfigDefault>();
				configMap.put(configItem.getType(), mapConfig);
			}

			if (configSelections.containsKey(configItem.getType())) {
				vConfigSelection = configSelections.get(configItem.getType());
			} else {
				vConfigSelection = new Vector<String>();
				configSelections.put(configItem.getType(), vConfigSelection);
			}

			StringBuilder configSelection = new StringBuilder(configItem
					.getConfigValue());
			configSelection.append("|");
			configSelection.append(configItem.getConfigKey());

			vConfigSelection.add(configSelection.toString());

			mapConfig.put(configItem.getConfigKey(), configItem);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getCurrentPageNameParent() {
		return currentPageNameParent;
	}

	public void setCurrentPageNameParent(String currentPageNameParent) {
		this.currentPageNameParent = currentPageNameParent;
	}

	/*
	 * private void addConfig(ViewEntry entry) { try { String type = (String)
	 * entry.getColumnValues().elementAt(0); String key = (String)
	 * entry.getColumnValues().elementAt(2); String value = (String)
	 * entry.getColumnValues().elementAt(3); String params = (String)
	 * entry.getColumnValues().elementAt(5);
	 * 
	 * addConfig(type, key, value, params);
	 * 
	 * } catch (Exception e) { Util.logError(e); } }
	 */
}
