package com.idealfed.worklogs.poc.craft;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.ArrayUtils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.util.concurrent.*;
import java.sql.Timestamp;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.SessionTrackingMode;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.http.Cookie;

import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;
import javax.mail.BodyPart;
import javax.mail.internet.MimeBodyPart;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.mail.Email;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.json.JSONEscaper;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
//import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.security.JiraAuthenticationContext;

import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.google.common.collect.Maps;
import com.idealfed.worklogs.jiraforms.ao.*;

import net.java.ao.Query;

public class Craft extends HttpServlet
{
	private final String productName = "iftWorklogs";
	private final String productRepo = "https://www.idealfed.com/dev10";


    //@ComponentImport
    private final PluginLicenseManager pluginLicenseManager;

	private final AttachmentManager attachmentManager;
	private final UserManager userManager;
	private final com.atlassian.jira.user.util.UserManager userManager2;
	private final GroupManager groupManager;
	private final LoginUriProvider loginUriProvider;
	private final TemplateRenderer templateRenderer;
	private final PluginSettingsFactory pluginSettingsFactory;
    private static final Logger plog = LogManager.getLogger("atlassian.plugin");
    private final ActiveObjects ao;
    private final JiraAuthenticationContext jiraAuthenticationContext;

	public Craft(PluginLicenseManager pluginLicenseManager, ActiveObjects ao, UserManager userManager, com.atlassian.jira.user.util.UserManager userManager2, GroupManager groupManager, LoginUriProvider loginUriProvider, TemplateRenderer templateRenderer, PluginSettingsFactory pluginSettingsFactory, AttachmentManager attachmentManager,JiraAuthenticationContext jiraAuthenticationContext) {
		this.jiraAuthenticationContext = jiraAuthenticationContext;
		this.attachmentManager = attachmentManager;
        this.pluginLicenseManager = pluginLicenseManager;
		this.userManager = userManager;
		this.userManager2 = userManager2;
		this.groupManager = groupManager;
		this.loginUriProvider = loginUriProvider;
		this.templateRenderer = templateRenderer;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.ao = checkNotNull(ao);
	}

	private String sanitize(String inString)
	{
		String wString = inString;

		wString=wString.replace("<","");
		wString=wString.replace(">","");
		wString=wString.replace(";","");
		wString=wString.replace("/","");
		wString=wString.replace("\"","");

		wString=wString.replace("%3c","");
    	wString=wString.replace("%3e","");
    	wString=wString.replace("%3b","");
    	wString=wString.replace("%2f","");
    	wString=wString.replace("%22","");

		wString=wString.replace("%3C","");
		wString=wString.replace("%3E","");
		wString=wString.replace("%3B","");
		wString=wString.replace("%2F","");
    	wString=wString.replace("%22","");

		return wString;
	}


	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {


    	String username = userManager.getRemoteUsername(request);

//section to comment or uncomment license

/*
		if (pluginLicenseManager.getLicense().isDefined())
		{
		   PluginLicense license = pluginLicenseManager.getLicense().get();
		   if (license.getError().isDefined())
		   {
				// handle license error scenario
				// (e.g., expiration or user mismatch)
				PrintWriter w = response.getWriter();
				w.println("Sorry, it appears your Ideal Forms for JIRA License is invalid, please update your license key.");
				w.close();
				return;
		   }
		}
		else
		{
				// handle unlicensed scenario
            PrintWriter w = response.getWriter();
            w.println("Sorry, it appears your Ideal Forms for JIRA License is missing, please update your license key.");
            w.close();
            return;
		}
*/

//end comment section


		String contextPath = request.getRequestURI();
		contextPath = contextPath.replace("/plugins/servlet/iftwl","");


    	String iwfAction = request.getParameter("ijfAction");
    	if(iwfAction==null) iwfAction="noAction";

    	String remote = request.getParameter("remote");
    	if(remote==null) remote="";
    	String itemId = request.getParameter("itemId");
    	if(itemId==null) itemId="";
    	String formId = request.getParameter("formId");
    	if(formId==null) formId="";
    	String craftFlag = request.getParameter("craft");
    	if(craftFlag==null) craftFlag="";
    	String debugFlag = request.getParameter("debug");
    	if(debugFlag==null) debugFlag="";
    	String decorator = request.getParameter("decorator");
    	if(decorator==null) decorator="";
    	String gVersionNum = request.getParameter("version");
    	if(gVersionNum==null) gVersionNum="0";
    	String themeFlag = request.getParameter("theme");
    	if(themeFlag==null) themeFlag="";
    	String groupName = request.getParameter("groupName");
    	if(groupName==null) groupName="";
    	String modeName = request.getParameter("mode");
    	if(modeName==null) modeName="";
    	String extVer = request.getParameter("extv");
    	if(extVer==null) extVer="";

		//XSS cleans
    	remote=sanitize(remote);
    	itemId=sanitize(itemId);
    	formId=sanitize(formId);
    	craftFlag=sanitize(craftFlag);
    	debugFlag=sanitize(debugFlag);
    	decorator=sanitize(decorator);
    	gVersionNum=sanitize(gVersionNum);



//comment for unlicensed running
//determine if Admin call or a Craft call, either way, require Administrator....

/*
        if(iwfAction.equals("noAction"))
        {
			if ((craftFlag.equals("true")) || (formId.equals("")))
			{
				if (username == null)
				{
						final PrintWriter w = response.getWriter();
						w.printf("{\"status\":\"NOSESSION\"}");
						w.close();
						return;
				}
				if (!userManager.isSystemAdmin(username))
				{
						final PrintWriter w = response.getWriter();
						w.printf("{\"status\":\"INVALIDSESSION\"}");
						w.close();
						return;
				}
			}
	    }
*/

//end comment section



    	String outTemplate = "main";
		if(extVer.equals("6")) outTemplate="main_6";
    	if (craftFlag.equals("true"))
    	{
    		outTemplate="craft";
    	}
    	else
    	{
			if (decorator.equals("general"))
			{
				outTemplate="main_general";
			}
			if (decorator.equals("admin"))
			{
				outTemplate="main_admin";
			}
		}
		if(!remote.equals(""))
		{

			if(remote.equals("libpm"))
			{
				outTemplate+="_remotepm";
			}
			else if(remote.equals("libjh"))
			{
				outTemplate+="_remotejh";
			}
			else if(remote.equals("libde"))
			{
				outTemplate+="_remotede";
			}
			else
			{
				outTemplate+="_remote";
			}
		}
		outTemplate+=".vm";

		if((modeName=="") && (formId==""))
		{
			outTemplate="splash.vm";
		}

		if((modeName!="") && (formId==""))
		{
			if(remote.equals(""))
				if(modeName.equals("versionadmin"))
				{
					if (decorator.equals("admin"))
					{
						outTemplate="main_admin.vm";
					}
					else
					{
						outTemplate="main.vm";
					}

				}
				else
				{
					plog.info("modeName is NOT versionadmin, decorator is: " + decorator);

					if (decorator.equals("admin"))
					{
						outTemplate="admin.vm";
					}
					else
					{
						outTemplate="admin_nodec.vm";
					}
				}
			else
			{
				outTemplate="admin_remote.vm";
			}
		}

		if(themeFlag.equals("crisp"))
		{
			outTemplate="maincrisp.vm";
			if(extVer.equals("7")) outTemplate="maincrisp_7.vm";
		}

		plog.info("Remote is:>" + remote + "<");
		plog.info("modeName is:>" + modeName + "<");
		plog.info("Template out is:>" + outTemplate + "<");




    	//you need to know if this is an anonymous call, if so, it can run....
    	//need to get the form....
    	boolean anon = false;
    	try
    	{
	    	for (Form f : ao.find(Form.class))
			{
				if(f.getName().equals(formId))
				{
					if(f.getFormAnon().equals("true"))
					{
						anon=true;
					}
					break;
				}
		    }
    	}
    	catch(Exception e)
    	{
    		anon=false;
    	}
    	if ((username == null) && (anon=false))
    	{
    		redirectToLogin(request, response);
    		return;
    	}

    	if ((craftFlag.equals("true")) && (!userManager.isSystemAdmin(username)))
    	{
			if(anon==false)
			{
	    		templateRenderer.render("nopermission.vm", null, response.getWriter());
	    		return;
			}
    	}
 		//proxy http request call
		if(iwfAction.equals("proxyHttpRequest"))
		{
			try
			{
				String targetUrl = java.net.URLDecoder.decode(request.getParameter("url"));

				if(checkUrlWhitelist(targetUrl)==false)
				{
					final PrintWriter w = response.getWriter();
				    w.print("Invalid URL request");
 				    w.close();
					return;
				}

				plog.info("Calling URL: " + targetUrl);



				URL url = new URL(targetUrl);
				HttpURLConnection.setFollowRedirects(false);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");

				int status = con.getResponseCode();

				plog.info("Api call response code: " + status);

				//add special handler for 400 responses, do not return result due to x attack
				if(status==400)
				{
					final PrintWriter w = response.getWriter();
					w.print("Remote server responded with invalid URL, response code: " + status);
					w.close();
					return;
				}


				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				con.disconnect();

				String sessionKey = request.getParameter("sessionKey");
                if(sessionKey!=null)
                {
					String cookies = con.getHeaderField("Set-Cookie");
					plog.info("cookie is : " + cookies);
					request.getSession().setAttribute(sessionKey,cookies);
				}

				final PrintWriter w = response.getWriter();
				w.print(content.toString());
				w.close();
				return;
			}
			catch(Exception e)
			{
				plog.error("Failed to get api proxy call: " + e.getMessage());
				final PrintWriter w = response.getWriter();
				w.print("Failed call: " + e.getMessage());
				w.close();
				return;
			}
		}

 		//proxyApi call
		if(iwfAction.equals("proxyApiCall"))
		{
			try
			{
				//plog.error("Calling proxy");

				String targetUrl = java.net.URLDecoder.decode(request.getParameter("url"));
				String formSetId = request.getParameter("formSetId");
				//plog.error("Have params");
				FormSet fs = ao.get(FormSet.class, new Integer(formSetId).intValue());

				if(fs==null)
				{
					final PrintWriter w = response.getWriter();
					w.print("Invalid formset configuration");
					w.close();
					return;
				}

				String fSettings = fs.getSettings();
				//plog.error("Proxy password: " + fSettings);
				if(fSettings.indexOf("proxyPassword")<1)
				{
					final PrintWriter w = response.getWriter();
					w.print("No proxy password configured");
					w.close();
					return;
				}

				String proxyServer = "";
				Pattern MY_PATTERN = Pattern.compile("(?<=proxyServer\\\\\",\\\\\"value\\\\\":\\\\\")(.*?)(?=\\\\\",\\\\\"comment)");
				Matcher m = MY_PATTERN.matcher(fSettings);
				while (m.find()) {
					proxyServer = m.group(1);
				}

				//look for cookie in session
				Object sCookie = request.getSession().getAttribute("sessionCookie");
				String jSessionId = "tbd";

				String cookie = "";
				if(sCookie==null)
				{
					String proxyUser = "";
					String proxyPass = "";
					MY_PATTERN = Pattern.compile("(?<=proxyUsername\\\\\",\\\\\"value\\\\\":\\\\\")(.*?)(?=\\\\\",\\\\\"comment)");
					m = MY_PATTERN.matcher(fSettings);
					while (m.find()) {
						proxyUser = m.group(1);
					}
					MY_PATTERN = Pattern.compile("(?<=proxyPassword\\\\\",\\\\\"value\\\\\":\\\\\")(.*?)(?=\\\\\",\\\\\"comment)");
					m = MY_PATTERN.matcher(fSettings);
					while (m.find()) {
						proxyPass = m.group(1);
					}
					//(?<=proxyUsername\\",\\"value\\":\\")(.*)(?=\\",\\"comment)
					//(?<=proxyPassword\\",\\"value\\":\\")(.*)(?=\\",\\"comment)

					//need to pull username and password from the formset settings...
					//plog.error("Proxy server: " + proxyServer);
					//plog.error("Proxy username: " + proxyUser);
					//plog.error("Proxy password: " + proxyPass);

					//perform login, get session cookie, store in header
					URL urlSession = new URL(proxyServer + "/rest/auth/1/session");
					String creds ="{\"username\":\""+proxyUser+"\",\"password\":\""+proxyPass+"\"}";
					HttpURLConnection conSession = (HttpURLConnection) urlSession.openConnection();
					conSession.setRequestProperty("Content-Type", "application/json; charset=utf-8");
					conSession.setRequestProperty("X-Atlassian-Token", "no-check");
					conSession.setRequestMethod("POST");

					conSession.setDoOutput(true);
					DataOutputStream out = new DataOutputStream(conSession.getOutputStream());
					out.writeBytes(creds);
					out.flush();
					out.close();

					BufferedReader ins = new BufferedReader(new InputStreamReader(conSession.getInputStream()));
					String inputLines;
					StringBuffer contents = new StringBuffer();
					while ((inputLines = ins.readLine()) != null) {
						contents.append(inputLines);
					}
					ins.close();
					conSession.disconnect();
					//plog.error("Content is: " + contents.toString());


					MY_PATTERN = Pattern.compile("(?<=JSESSIONID\",\"value\":\")(.*?)(?=\"},)");
							m = MY_PATTERN.matcher(contents.toString());
							while (m.find()) {
								jSessionId = m.group(1);
					}

					request.getSession().setAttribute("sessionCookie",jSessionId);
					//plog.error("jSession is: " + jSessionId);
				}
				else{
					jSessionId=sCookie.toString();
					//plog.error("jSession from memory is: " + jSessionId);
				}

				//look for additional params, if there, suffix to the targetUrl.  (for itemLists)
		    	String start = request.getParameter("start");
		    	String startAt = request.getParameter("startAt");
		    	String maxResults = request.getParameter("maxResults");
		    	String page = request.getParameter("page");
		    	String suffix = "";
		    	if(start!=null)
		    	{
					suffix = "&start="+start+"&startAt="+startAt+"&maxResults="+maxResults+"&page="+page;
					targetUrl = targetUrl + suffix;
				}

				//cookie aquired, now call API
				URL url = new URL(proxyServer + targetUrl);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				con.setRequestProperty("X-Atlassian-Token", "no-check");
				con.setRequestProperty("Cookie", "JSESSIONID=" + jSessionId);
				//set cookie...

				int status = con.getResponseCode();
				//plog.error("Api call response code: " + status);

				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				con.disconnect();
				//plog.error("Have response");
				response.setContentType("application/json; charset=utf-8");
				final PrintWriter w = response.getWriter();
				w.print(content.toString());
				w.close();
				return;
			}
			catch(Exception e)
			{
				plog.error("Failed to get api proxy call: " + e.getMessage());
				final PrintWriter w = response.getWriter();
				w.print("Failed call: " + e.getMessage());
				w.close();
				return;
			}
		}

    	if(iwfAction.equals("getGroupMembers"))
    	{
			try
			{
				StringBuilder retS = new StringBuilder();
				if(this.groupManager.groupExists(groupName)==true)
				{
					//		retS.append("{\"status\":\"" + ct.getID() + "\",");
					Collection<ApplicationUser>  groupUsers = this.groupManager.getUsersInGroup(groupName, false);
					retS.append("{\"status\":\"OK\",\"message\":\"\",\"results\":[");
					boolean isFirst = true;
					for (ApplicationUser usr : groupUsers)
					{
						if(isFirst==true)
						{
							retS.append("{\"name\":\""+usr.getName()+"\",\"displayName\":\""+usr.getDisplayName()+"\",\"email\":\""+usr.getEmailAddress()+"\"}");
							isFirst=false;
						}
						else
						{
							retS.append(",{\"name\":\""+usr.getName()+"\",\"displayName\":\""+usr.getDisplayName()+"\",\"email\":\""+usr.getEmailAddress()+"\"}");
						}
					}
					retS.append("]}");
				}
				else
				{
					retS.append("{\"status\":\"error\",\"message\":\"Group does not exist: "+groupName+"\"}");
				}
				final PrintWriter w = response.getWriter();
				w.print(retS.toString());
				w.close();
				return;
			}
			catch(Exception e)
			{
				plog.error("Failed to get api group members: " + e.getMessage());
				final PrintWriter w = response.getWriter();
				w.print("Failed to get api group members: " + e.getMessage());
				w.close();
				return;
			}
		}

    	if(iwfAction.equals("copyIssueAttachments"))
    	{

			try
			{


				StringBuilder retS = new StringBuilder();
				String attString = request.getParameter("attachments");
				String targetIssue = request.getParameter("toIssueKey");
				String attArray[] = attString.split(",");
				boolean goodAtts = true;

				ApplicationUser au =  jiraAuthenticationContext.getUser();// userManager2.getUserByName(username);
				Attachment attachment=null;
				plog.info("About to copy a jira attachments " +attString + " to " + targetIssue + " under username " + username);

				for(int i=0;i<attArray.length;i++)
				{
					try
					{
						attachment = attachmentManager.getAttachment(Long.parseLong(attArray[i]));

						plog.info("Verifying type attachment: " + attachment.getClass().toString());
						plog.info("Verifying type user: " + au.getClass().toString());
						plog.info("Verifying type attMng: " + attachmentManager.getClass().toString());
                        plog.info("Copying file " + attachment.getFilename() + " by " + au.getUsername() + " to issue " + targetIssue);

						Object cpRes = attachmentManager.copyAttachment(attachment, au, targetIssue);
						//attachmentManager.copyAttachment(att,currentUser,"TPO-2");
                        retS.append(attArray[i] + " copied\n");
					}
					catch(Exception ae)
					{
						goodAtts=false;
						retS.append(attArray[i] + " had error: " + ae.getMessage() + "\n");
						continue;
					}
				}
				String returnStr = "";
				if(goodAtts==true)
				{
					returnStr = "{\"status\":\"success\",\"message\":\""+retS.toString()+"\"}";
				}
				else
				{
					returnStr = "{\"status\":\"error\",\"message\":\""+retS.toString()+"\"}";
				}
				final PrintWriter w = response.getWriter();
				w.print(returnStr);
				w.close();
				return;
			}
			catch(Exception e)
			{
				plog.error("Failed to copy issue attachments: " + e.getMessage());
				final PrintWriter w = response.getWriter();
				w.print("{\"status\":\"error\",\"message\":\""+e.getMessage()+"\"}");
				w.close();
				return;
			}
		}


    	if(iwfAction.equals("getFormConfig"))
    	{

        		//we need to get the form -> formSet
        		FormSet fs = null;
        		for (Form f : ao.find(Form.class))
                {
                    if(f.getName().equals(formId)) fs = f.getFormSet();
                }

	    		final PrintWriter w = response.getWriter();
	    		w.printf("{\"status\":\"OK\",\"resultSet\":[");
	    		if(fs!=null)   w.printf(getAoFormSetJson(fs));
	    		w.printf("{}],\"customTypes\":[");

	    		for (CustomType ct : ao.find(CustomType.class))
	            {
	                w.printf(getCustomTypeConfig(ct));
	            }

	    		w.printf("{}]}");

	    		w.close();
	    		return;

		}


    	if(iwfAction.equals("getConfig"))
    	{
    		//pull the config from AO and return...
    		if(gVersionNum.equals("0"))
    		{
	    		final PrintWriter w = response.getWriter();
	    		w.printf("{\"status\":\"OK\",\"resultSet\":[");

	    		for (FormSet fs : ao.find(FormSet.class))
	            {
	                w.printf(getAoFormSetJson(fs));
	            }
	    		w.printf("{}],\"customTypes\":[");

	    		for (CustomType ct : ao.find(CustomType.class))
	            {
					if(ct!=null) w.printf(getCustomTypeJson(ct));
	            }
	    		w.printf("{}]}");

	    		w.close();
	    		return;
    		}
    		else
    		{
    			//pull the called version
    			int vId = new Integer(gVersionNum).intValue();
    				//formSet must exist by ID and we need it....
    				//OK, now get the object by ID
    			Version v = ao.get(Version.class, vId);
            	final PrintWriter w = response.getWriter();
            	w.printf(v.getConfig());
        		w.close();
        		return;

    		}
    	}
    	else if(iwfAction.equals("getVersions"))
    	{
    		//pull the config from AO and return...
    		final PrintWriter w = response.getWriter();
    		w.printf("{\"status\":\"OK\",\"resultSet\":[");
    		for (Version v : ao.find(Version.class, Query.select().limit(25).order("ID DESC")))
            {
                w.printf("{\"id\":\""+v.getID()+"\",\"created\":\""+v.getDate()+"\",\"author\":\""+v.getAuthor()+"\"},");
            }
    		w.printf("{}]}");
    		w.close();
    		return;
    	}
    	else if(iwfAction.equals("getVersion"))
    	{
        	String versionId = request.getParameter("versionId");
			int vId = new Integer(versionId).intValue();
				//formSet must exist by ID and we need it....
				//OK, now get the object by ID
			Version v = ao.get(Version.class, vId);
        	final PrintWriter w = response.getWriter();
        	w.printf(v.getConfig());
    		w.close();
    		return;
    	}
    	else if(iwfAction.equals("getCustomType"))
    	{
			try
			{
				String customTypeId = request.getParameter("customTypeId");
				int ctId = new Integer(customTypeId).intValue();
					//formSet must exist by ID and we need it....
					//OK, now get the object by ID
				CustomType ct = ao.get(CustomType.class, ctId);

				StringBuilder sb = new StringBuilder();
				sb.append("{\"id\":\"" + ct.getID() + "\",");
				sb.append("\"name\":\"" + ct.getName() + "\",");
				sb.append("\"description\":\"" + ct.getDescription() + "\",");
				sb.append("\"customType\":\"" + ct.getCustomType() + "\",");
				sb.append("\"fieldName\":\"" + ct.getFieldName() + "\",");
				sb.append("\"settings\":\"" + ct.getSettings() + "\"}");

				final PrintWriter w = response.getWriter();
				w.printf(sb.toString());
				w.close();
				return;
		    }
		    catch(Exception cte)
		    {
				final PrintWriter w = response.getWriter();
				w.printf("Error retrieving custom type");
				w.close();
				return;
			}
    	}
    	else if(iwfAction.equals("clearConfig"))
    	{
			if (!userManager.isSystemAdmin(username))
			{
					final PrintWriter w = response.getWriter();
					w.printf("{\"status\":\"INVALIDSESSION\"}");
					w.close();
					return;
			}
    		for (FormSet fs : ao.find(FormSet.class))
            {
    			for(Form f : fs.getForms())
    			{
    				ao.delete(f);
    			}
    			for(Snippet s : fs.getSnippets())
    			{
    				ao.delete(s);
    			}
                ao.delete(fs);
            }
            for (CustomType ct : ao.find(CustomType.class))
			{
				ao.delete(ct);
			}
			final PrintWriter w = response.getWriter();
				w.printf("OK");
				w.close();
    		return;
    	}
    	else
    	{
			String vInj = getVelocityInjections();

        	Map<String, Object> craft = Maps.newHashMap();
        	craft.put("ijfUsername",username);
        	craft.put("ijfExercise", "123");
        	craft.put("ijfVersion", "NA");
        	craft.put("ijfFormId", formId);
        	craft.put("ijfItemId", itemId);
        	craft.put("ijfDebug", debugFlag);
        	craft.put("ijfCraft", craftFlag);
        	craft.put("ijfRoot", contextPath);
        	craft.put("ijfRemote", remote);
        	craft.put("ijfHtmlReferences", vInj);
            craft.put("ijfProduct", productName);
            craft.put("ijfProductRepo", productRepo);


        	response.setContentType("text/html;charset=utf-8");

        	String snippets = "";
        	String styles = "";
        	String snippetPub = "";
        	if((!formId.equals("")) && (craftFlag.equals("")))
        	{
        		//we need to get the form -> formSet and render the javascript snippets it contains...
        		FormSet fs = null;
        		for (Form f : ao.find(Form.class))
                {
                    if(f.getName().equals(formId)) fs = f.getFormSet();
                }
        		if(fs != null)
        		{
        			try
        			{
	        			JSONObject sObject;
	        			StringBuilder snips = new StringBuilder();
	        			StringBuilder styls = new StringBuilder();
	        			StringBuilder sOut = new StringBuilder();
	        			for (Snippet s : fs.getSnippets())
	        			{
	        				if(s.getName().equals("style"))
	        				{
	        					String outStyle = "{\"snippet\":" + s.getSnippet() + "}";

	        					sObject = new JSONObject(outStyle);
	        					outStyle = sObject.getString("snippet");
	        					outStyle=outStyle.replace("~pct~", "%");
	        					styls.append(outStyle);
	        				}
	        				else
	        				{
	        					sOut.append("," + s.getName() + ":" + s.getName());

	        					String outSnip = "{\"snippet\":" + s.getSnippet() + "}";
	        					sObject = new JSONObject(outSnip);
	        					outSnip = sObject.getString("snippet");
	        					outSnip=outSnip.replace("~pct~", "%");
	        					snips.append("\n");
	        					snips.append(outSnip);
	        				}
	        			}
	        			snippets = snips.toString();
	        			styles = styls.toString();
	        			snippetPub = sOut.toString();
        			}
        			catch(JSONException e)
        			{
        				snippets = "//Failed to gen snippets: " + e.getMessage();
            			styles = "";
            			snippetPub = "";
        			}
        		}
        	}
         	craft.put("ijfSnippetPub", snippetPub);
        	craft.put("ijfScript", snippets);
        	craft.put("ijfStyle", styles);

        	templateRenderer.render(outTemplate, craft, response.getWriter());
    	}

    }

	private String getCustomTypeJson(CustomType ct)
	{
		String cId = "";
		String cName = "";
		try
		{
			cId = Integer.toString(ct.getID());
			cName = ct.getName();

			StringBuilder sb = new StringBuilder();
			sb.append("{\"id\":\"" + ct.getID() + "\",");
			sb.append("\"name\":\"" + ct.getName() + "\",");
			sb.append("\"description\":\"" + ct.getDescription() + "\",");
			sb.append("\"customType\":\"" + ct.getCustomType() + "\",");
			sb.append("\"fieldName\":\"" + ct.getFieldName() + "\",");

			String cType = ct.getCustomType();
			if(cType.equals("FILE"))
			{
						sb.append("\"settings\":\"\"[]\"\"},");
			}
			else
			{
						sb.append("\"settings\":\"" + ct.getSettings() + "\"},");
			}

			return sb.toString();
		}
		catch(Exception e)
		{
 		    plog.error("Failed to load custom type: " + cName + " " + e.getMessage());
			return "";
		}
	}

	private String getCustomTypeConfig(CustomType ct)
	{
		String cId = "";
		String cName = "";
		try
		{
			cId = Integer.toString(ct.getID());
			cName = ct.getName();

			StringBuilder sb = new StringBuilder();
			sb.append("{\"id\":\"" + ct.getID() + "\",");
			sb.append("\"name\":\"" + ct.getName() + "\",");
			sb.append("\"description\":\"" + ct.getDescription() + "\",");
			sb.append("\"customType\":\"" + ct.getCustomType() + "\",");
			sb.append("\"fieldName\":\"" + ct.getFieldName() + "\"");

			String cType = ct.getCustomType();
			if(cType.equals("FILE"))
			{
						sb.append(",\"settings\":\"\"[]\"\"},");
			}
			else
			{
						sb.append("},");
			}

			return sb.toString();
		}
		catch(Exception e)
		{
			plog.error("Failed to load custom type: " + cName + " " + e.getMessage());
			return "";
		}
	}

	private String getAoFormSetJson(FormSet fs)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{\"id\":\"" + fs.getID() + "\",");
		sb.append("\"name\":\"" + fs.getName() + "\",");
		sb.append("\"projectName\":\"" + fs.getProjectName() + "\",");
		sb.append("\"projectId\":\"" + fs.getProjectId() + "\",");


		sb.append("\"customerKey\":\"" + fs.getCustomerKey() + "\",");
		sb.append("\"iftFormGroup\":\"" + fs.getIftFormGroup() + "\",");
		sb.append("\"iftFormGroupVersion\":\"" + fs.getIftFormGroupVersion() + "\",");



		String sfSettings = fs.getSettings();
		sfSettings = sfSettings.replaceFirst("(?<=proxyPassword\\\\\",\\\\\"value\\\\\":\\\\\")(.*?)(?=\\\\\",\\\\\"comment)","hidden");
		sb.append("\"settings\":\"" + sfSettings + "\",");
		sb.append("\"forms\":[");
        for (Form f : fs.getForms()) // (2)
        {
        	 sb.append(getAoFormJson(f));
        }
		sb.append("{}],");

		sb.append("\"snippets\":[");
        for (Snippet s : fs.getSnippets()) // (2)
        {
        	 sb.append(getAoSnippetJson(s));
        }
		sb.append("{}]},");

		return sb.toString();
	}
	private String getAoFormJson(Form f)
	{
		if(f.getTestIssue().equals("")) f.setTestIssue("~");
		StringBuilder sb = new StringBuilder();
		sb.append("{\"id\":\"" + f.getID() + "\",");
		sb.append("\"name\":\"" + f.getName() + "\",");
		sb.append("\"testIssue\":\"" + f.getTestIssue() + "\",");
		sb.append("\"formAnon\":\"" + f.getFormAnon() + "\",");
		sb.append("\"formProxy\":\"" + f.getFormProxy() + "\",");
		sb.append("\"issueType\":\"" + f.getIssueType() + "\",");
		sb.append("\"formType\":\"" + f.getFormType() + "\",");
		sb.append("\"settings\":\"" + f.getSettings() + "\",");
		sb.append("\"fields\":\"" + f.getFields() + "\"},");
		return sb.toString();
	}
	private String getAoSnippetJson(Snippet s)
	{
	  try
	  {
		if(s.getName().equals("")) s.setName("~");
		if(s.getSnippet().equals("")) s.setSnippet("~");
		StringBuilder sb = new StringBuilder();
		sb.append("{\"id\":\"" + s.getID() + "\",");
		sb.append("\"name\":\"" + s.getName() + "\",");
		sb.append("\"snippet\":\"" + s.getSnippet() + "\"},");
		return sb.toString();
	  }
	  catch(Exception e)
	  {
		  plog.error("Failed to get snippet: " + e.getMessage());
		  return "{}";
	  }
	}

    public int compareVersions(Version lhs, Version rhs) {
					// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
					return lhs.getID() > rhs.getID() ? -1 : (lhs.getID() < rhs.getID()) ? 1 : 0;
 	}
    private void cleanVersions(int keepNum)
    {
		try
		{
			int maxVersion = 0;
			for (Version v : ao.find(Version.class, Query.select().limit(1).order("ID DESC")))
			{
				maxVersion = v.getID();
			}
			maxVersion = maxVersion - keepNum;
			plog.debug("deleting versions older than " + maxVersion);
			ao.deleteWithSQL(Version.class, "\"ID\" < ?", maxVersion);
		}
		catch(Exception ve)
		{
		  plog.error("Failed to get version: " + ve.getMessage());
		}
    }

    private boolean checkUrlWhitelist(String inUrl)
    {
		try
		{
			//lookup custom type, verify URL is in the list....
			boolean urlOk = false;
			CustomType ct = null;
			for (CustomType t : ao.find(CustomType.class))
			{
				if(t.getName().equals("Proxy Whitelist")) ct = t;
			}
			if(ct==null)
			{
				return true;
			}
			String proxyList = ct.getSettings();
            String outStyle = "{\"whitelist\":" + proxyList + "}";
            JSONObject sObject;
			sObject = new JSONObject(outStyle);
            String whiteList = sObject.getString("whitelist");
            whiteList=whiteList.substring(1,whiteList.length()-1);
			String okUrls[] = whiteList.split("\\\\n");
			for(int i=0;i<okUrls.length;i++)
			{
				//plog.error("Testing URL: " + okUrls[i]);
				if(inUrl.indexOf(okUrls[i]) >-1) urlOk=true;
			}
			return urlOk;
		}
		catch(Exception e)
		{
			plog.error("Error getting Proxy Whitelist "  + e.getMessage());
			return false;
		}
	}


    private String getVelocityInjections()
    {
		try
		{
			//lookup custom type, verify URL is in the list....
			CustomType ct = null;
			String retStr = "";
			for (CustomType t : ao.find(CustomType.class))
			{
				if(t.getName().equals("HTML References")) ct = t;
			}
			if(ct==null)
			{
				return "";
			}
			String proxyList = ct.getSettings();
            String tempRefs = "{\"references\":" + proxyList + "}";
            JSONObject sObject;
			sObject = new JSONObject(tempRefs);
            String refs = sObject.getString("references");
            refs=refs.substring(1,refs.length()-1);
            String refLines[] = refs.split("\\\\n");
			for(int i=0;i<refLines.length;i++)
			{
				//plog.error("Testing URL: " + okUrls[i]);
				retStr += refLines[i].replaceAll("\\\\\\\"","\"") + "\n";
			}
			return retStr;
		}
		catch(Exception e)
		{
			plog.error("Error getting Proxy Whitelist "  + e.getMessage());
			return "";
		}
	}




	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {


		//only admin can be in here...
     	String username = userManager.getRemoteUsername(req);
		if (username == null)
		{
	    		final PrintWriter w = res.getWriter();
	    		w.print("{\"status\":\"NOSESSION\"}");
	    		w.close();
	    		return;
    	}
 		String iwfAction = req.getParameter("action");
		if(iwfAction==null) iwfAction="noAction";


    	//proxy call
		if(iwfAction.equals("proxyCall"))
		{
			try
			{
				//plog.error("Calling proxy");

				String targetUrl = java.net.URLDecoder.decode(req.getParameter("url"));


				if(checkUrlWhitelist(targetUrl)==false)
				{
					final PrintWriter w = res.getWriter();
				    w.print("Invalid URL request");
 				    w.close();
					return;
				}


				plog.error("URL for proxy call: " + targetUrl);
				String targetMethod = req.getParameter("method");
				String targetData = req.getParameter("data");
				String contentType = req.getParameter("contenttype");
				String sessionKey = req.getParameter("sessionKey");

			    if(!targetMethod.equals("GET"))
			    {
					if(!targetMethod.equals("POST"))
					{
						if(!targetMethod.equals("PUT"))
						{
							if(!targetMethod.equals("DELETE"))
							{
								final PrintWriter w = res.getWriter();
								w.print("Invalid method request");
								w.close();
								return;
							}
						}
					}
				}

				//need to protect XSS in the form of nested scripts within "data"
				//data is POST or PUT content that will be passed on.  This data should never have script content
				//in it's current state, data should be urlDecoded. and can be cleaned as it is

				//removing because this was not the XSS issue and it causes errors with soap calls
				//targetData = sanitize(targetData);



				//plog.error("Have params");
//plog.error("Creating URL");
				URL url = new URL(targetUrl);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
//plog.error("Connection open");

				//handle cookie from session
				if(sessionKey!=null)
				{
					if(!sessionKey.equals(""))
					{
						Object sCookie = req.getSession().getAttribute(sessionKey);
						if(sCookie==null)
						{
							//this requires a session to call, return error with no session
							plog.error("Required session key not set");
							final PrintWriter w = res.getWriter();
							w.print("No session established for session key");
							w.close();
							return;
						}
						String jSessionId=sCookie.toString();
//	plog.error("Cookie set to: " + jSessionId);

						con.setRequestProperty("Cookie", jSessionId);
					}
				}

				if(contentType!=null)
				{
					con.setRequestProperty("Content-Type", contentType);
				}
				con.setRequestMethod(targetMethod);
				plog.info("method set");

				if((targetMethod.equals("POST")) || (targetMethod.equals("PUT")))
				{
					//targetData=targetData;
					con.setDoOutput(true);
					DataOutputStream out = new DataOutputStream(con.getOutputStream());
					out.writeBytes(targetData);
					out.flush();
					out.close();
				}

				int status = con.getResponseCode();
//plog.error("status is: " + status);

				//add special handler for 400 responses, do not return result due to x attack
				if(status==400)
				{
					final PrintWriter w = res.getWriter();
					w.print("Remote server responded with invalid URL, response code: " + status);
					w.close();
					return;
				}


				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				con.disconnect();
//plog.error("writing output");
				//plog.error("Have response");
				final PrintWriter w = res.getWriter();
				w.print(content.toString());
				w.close();
				return;
			}
			catch(Exception e)
			{
				plog.error("Failed to get proxy call: " + e.getMessage());
				final PrintWriter w = res.getWriter();
				w.print("Exception occurred on procy call, refer to log.");
				w.close();
				return;
			}
		}





		if(iwfAction.equals("sendMail"))
		{
			String toAddresses = req.getParameter("targets");
			String subject = req.getParameter("subject");
			String body = req.getParameter("body");
			String html = req.getParameter("html");
			String attString = req.getParameter("attachments");
			String targets[] = toAddresses.split(",");
			try
			{
				 SMTPMailServer  mailServer = MailFactory.getServerManager().getDefaultSMTPMailServer();
				 Email email = null;
 		    	 if(attString==null)
 		    	 {
					 //no attachments
					 email = new Email("tbd@tbd.com").setSubject(subject).setBody(body);
					 email.setMimeType("text/html");
					 if (html.equals("true")) email.setMimeType("text/html");
				 }
				 else
				 {
					 //get atts....
	  			    String attArray[] = attString.split(",");

					Multipart multipart = new MimeMultipart("mixed");
			        BodyPart attachBody = null;

				   boolean goodAtts = true;
				   File attachedFile=null;
				   FileDataSource source=null;
				   String attachedFilename=null;
				   Attachment attachment=null;
				   for(int i=0;i<attArray.length;i++)
				   {
					   try
					   {
							attachment = attachmentManager.getAttachment(Long.parseLong(attArray[i]));
						}
						catch(Exception ae)
						{
							goodAtts=false;
							continue;
						}
						attachedFile  = AttachmentUtils.getAttachmentFile(attachment);
						attachedFilename = attachment.getFilename();
						source = new FileDataSource(attachedFile);
						attachBody = new MimeBodyPart();
						attachBody.setDataHandler(new DataHandler(source));
						attachBody.setFileName(attachedFilename);
						multipart.addBodyPart(attachBody);
				    }


					 email = new Email("tbd@tbd.com");
					 email.setSubject(subject);
					 email.setMimeType("text/html");
					 if (html.equals("true")) email.setMimeType("text/html");
			         if(goodAtts)
			         {
						 email.setMultipart(multipart);
					 }
					 else
					 {
						 body = body + "<br>\n<br>\nError including attachments";
					 }
					 email.setBody(body);
				 }



				 //no changes yet
				   for(int i=0;i<targets.length;i++)
				   {
					    email.setTo(targets[i]);
  					    mailServer.send(email);
						//SMTPMailServer  mailServer = MailFactory.getServerManager().getDefaultSMTPMailServer();
						//if (html.equals("true"))
						//	mailServer.send(new Email(targets[i]).setSubject(subject).setBody(body).setMimeType("text/html"));
						//else
						//	mailServer.send(new Email(targets[i]).setSubject(subject).setBody(body));
				   }
				final PrintWriter w = res.getWriter();
				w.print("sent");
				w.close();
				return;
			}
			catch(Exception e)
			{
				final PrintWriter w = res.getWriter();
				w.print("Failed to send: " + e.getMessage());
				w.close();
				return;
			}
		}


    	String inJson = req.getParameter("jsonConfig");
		if(iwfAction.equals("saveCustomType"))
		{
			//formset exists.  And, form exists...so, use the get by ID and update the values....

			//TODO:  add group filter that indicates who can edit what custom type references

			try
			{
				JSONObject inType = new JSONObject(inJson);

				CustomType ct;

				int ctId = new Integer(inType.getString("customTypeId")).intValue();
				if(ctId==0)
				{
					//formSet must exist by ID and we need it....
					//OK, now get the object by ID
					String ctName = inType.getString("name");
					if(ctName!=null)
					{
						ct =  ao.create(CustomType.class);
						ct.setName(inType.getString("name"));
						ct.setDescription(inType.getString("description"));
						ct.setCustomType(inType.getString("customType"));
						ct.setFieldName(inType.getString("fieldName"));
						ct.setSettings(inType.getString("settings"));
						ct.save();
						final PrintWriter w = res.getWriter();
						w.printf("{\"status\":\"OK\",\"result\":\""+ct.getID()+"\"}");
						w.close();
						return;
					}
					else
					{
						final PrintWriter w = res.getWriter();
						w.printf("{\"status\":\"Error\",\"result\":\"Name was null on attempted save\"}");
						w.close();
						return;
					}
				}
				else
				{
					//OK, now get the object by ID
					ct = ao.get(CustomType.class, ctId);
					ct.setName(inType.getString("name"));
					ct.setDescription(inType.getString("description"));
					ct.setCustomType(inType.getString("customType"));
					ct.setFieldName(inType.getString("fieldName"));
					ct.setSettings(inType.getString("settings"));
					ct.save();
					final PrintWriter w = res.getWriter();
					w.printf("{\"status\":\"OK\",\"result\":\""+ct.getID()+"\"}");
					w.close();
					return;
				}



			}
			catch(JSONException e)
			{
				//failed json read
				plog.error("Failed to save custom type" + e.getMessage());
				final PrintWriter w = res.getWriter();
				w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
				w.close();
				return;
			}
    	}



		//end public POST methods



    	if (!userManager.isSystemAdmin(username))
    	{
				final PrintWriter w = res.getWriter();
				w.printf("{\"status\":\"INVALIDSESSION\"}");
				w.close();
	    		return;
		}

    	if(iwfAction.equals("saveFormBasic"))
    	{

    		//formset exists.  And, form exists...so, use the get by ID and update the values....
    		try
    		{
	    		JSONObject inForm = new JSONObject(inJson);
	    		//JSONObject formSettings = inForm.getJSONObject("settings");
	    		//JSONObject formFields = inForm.getJSONObject("fields");
	    		FormSet fs = ao.get(FormSet.class, new Integer(inForm.getString("formSetId")).intValue());
				if(fs==null) throw new JSONException("Failed to get formset");
				//this may be a new add or a save...if Form ID == 0 it's a new add, look for that first...
				Form f;
				int formId = new Integer(inForm.getString("formId")).intValue();
				if(formId==0)
				{
					//formSet must exist by ID and we need it....
					//OK, now get the object by ID
					f = ao.create(Form.class);
		        	f.setFormSet(fs);
				}
				else
				{
					//OK, now get the object by ID
					f = ao.get(Form.class, formId);
					f.setFormSet(fs);
				}

	    		f.setName(inForm.getString("formName"));
        		f.setIssueType(inForm.getString("issueType"));
        		f.setTestIssue(inForm.getString("testIssue"));
        		if(inForm.has("formAnon")) f.setFormAnon(inForm.getString("formAnon"));
        		if(inForm.has("formProxy")) f.setFormProxy(inForm.getString("formProxy"));
        		f.setFormType(inForm.getString("formType"));
	    		f.save();

	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save forms config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("saveFormConfig"))
    	{
    		try
    		{

				//backup the existing configuration first
				StringBuffer cConfig = new StringBuffer();
				cConfig.append("{\"status\":\"OK\",\"resultSet\":[");
				for (FormSet fs : ao.find(FormSet.class))
				{
					cConfig.append(getAoFormSetJson(fs));
				}
				cConfig.append("{}],\"customTypes\":[");
				for (CustomType ct : ao.find(CustomType.class))
				{
					cConfig.append(getCustomTypeJson(ct));
				}
				cConfig.append("{}]}");


				Version v = ao.create(Version.class);
				v.setDate(new Date());
				v.setAuthor(userManager.getRemoteUsername(req));
				v.setConfig(cConfig.toString());
				v.save();
				cleanVersions(50);
plog.debug("Configurations cleaned");


				//formset exists.  And, form exists...so, use the get by ID and update the values....

	    		JSONObject inForm = new JSONObject(inJson);
	    		//JSONObject formSettings = inForm.getJSONObject("settings");
	    		//JSONObject formFields = inForm.getJSONObject("fields");

				//this may be a new add or a save...if Form ID == 0 it's a new add, look for that first...
				Form f;
				int formId = new Integer(inForm.getString("formId")).intValue();
				if(formId==0)
				{
					//formSet must exist by ID and we need it....
					//OK, now get the object by ID
					FormSet fs = ao.get(FormSet.class, new Integer(inForm.getString("formSetId")).intValue());
					if(fs==null) throw new JSONException("Failed to get formset");
					f = ao.create(Form.class);
		        	f.setFormSet(fs);
				}
				else
				{
					//OK, now get the object by ID
					f = ao.get(Form.class, formId);
				}

	    		f.setName(inForm.getString("formName"));
        		f.setTestIssue(inForm.getString("testIssue"));
        		f.setIssueType(inForm.getString("issueType"));
        		f.setFormType(inForm.getString("formType"));
        		if(inForm.has("formAnon")) f.setFormAnon(inForm.getString("formAnon"));
        		if(inForm.has("formProxy")) f.setFormProxy(inForm.getString("formProxy"));
	    		f.setSettings(inForm.getString("formSettings"));
	    		f.setFields(inForm.getString("fields"));
	    		f.save();
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save forms config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("deleteFormConfig"))
    	{
    		//formset exists.  And, form exists...so, use the get by ID and update the values....
    		try
    		{
	    		JSONObject inForm = new JSONObject(inJson);
				Form f;
				int formId = new Integer(inForm.getString("formId")).intValue();

					//OK, now get the object by ID
				f = ao.get(Form.class, formId);
				ao.delete(f);

	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save forms config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("saveFormSet"))
    	{
    		//formset exists.  And, form exists...so, use the get by ID and update the values....
    		try
    		{
	    		JSONObject inForm = new JSONObject(inJson);

				FormSet f;


				int fsId = new Integer(inForm.getString("formSetId")).intValue();
				if(fsId==0)
				{
					//formSet must exist by ID and we need it....
					//OK, now get the object by ID

					f =  ao.create(FormSet.class);
				}
				else
				{
					//OK, now get the object by ID
					f = ao.get(FormSet.class, fsId);
				}

	    		f.setName(inForm.getString("name"));
        		f.setProjectId(inForm.getString("projectId"));
        		f.setProjectName(inForm.getString("projectName"));

        		f.setCustomerKey(inForm.getString(""));
        		f.setIftFormGroup(inForm.getString("iftFormGroup"));
        		f.setIftFormGroupVersion(inForm.getString("iftFormGroupVersion"));

        		//if proxypassword = hidden, do not overwrite...,need to get the value from the
        		//existing form settings and set it here....

				String settingsToSave = inForm.getString("settings");
				if(settingsToSave.indexOf("hidden")>-1)
        		{
					//need to replace existing pw with this one....
					String fSettings = f.getSettings();
					String proxyPass ="";
					Pattern MY_PATTERN = Pattern.compile("(?<=proxyPassword\\\\\",\\\\\"value\\\\\":\\\\\")(.*?)(?=\\\\\",\\\\\"comment)");
					Matcher m = MY_PATTERN.matcher(fSettings);
					while (m.find()) {
						proxyPass = m.group(1);
					}
					if(!proxyPass.equals(""))
					{
						//replace "hidden
						settingsToSave = settingsToSave.replaceFirst("hidden",proxyPass);
					}
				}
        		f.setSettings(settingsToSave);
	    		//f.setSettings(inForm.getString("settings"));
	    		f.save();
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save form set config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("deleteCustomType"))
    	{
    		try
    		{
	    		JSONObject inForm = new JSONObject(inJson);
				CustomType ct;
				int ctId = new Integer(inForm.getString("customTypeId")).intValue();

					//OK, now get the object by ID
				ct = ao.get(CustomType.class, ctId);
				ao.delete(ct);

	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\",\"result\":\""+ctId+"\"}");
	    		w.close();
	    		return;
    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to delete type config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("deleteFormSet"))
    	{
    		//formset exists.  And, form exists...so, use the get by ID and update the values....
    		try
    		{
	    		JSONObject inForm = new JSONObject(inJson);

	    		FormSet f=null;
				int fsId = new Integer(inForm.getString("formSetId")).intValue();

				f = ao.get(FormSet.class, fsId);
		        for (Snippet s : f.getSnippets())
		        {
		        	 ao.delete(s);
		        }
				ao.delete(f);

	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save form set config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("saveSnippet"))
    	{
    		//formset exists.  And, form exists...so, use the get by ID and update the values....
    		try
    		{
	    		JSONObject inForm = new JSONObject(inJson);

				Snippet s;
				int snippetId = new Integer(inForm.getString("snippetId")).intValue();
				if(snippetId==0)
				{
					//formSet must exist by ID and we need it....
					//OK, now get the object by ID
					FormSet fs = ao.get(FormSet.class, new Integer(inForm.getString("formSetId")).intValue());
					if(fs==null) throw new JSONException("Failed to get formset");
					s = ao.create(Snippet.class);
		        	s.setFormSet(fs);
				}
				else
				{
					//OK, now get the object by ID
					s = ao.get(Snippet.class, snippetId);
				}

	    		s.setName(inForm.getString("name"));
        		s.setSnippet(inForm.getString("snippet"));
	    		s.save();
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\",\"result\":\""+s.getID()+"\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save forms config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("deleteSnippet"))
    	{
    		//formset exists.  And, form exists...so, use the get by ID and update the values....
    		try
    		{
	    		JSONObject inForm = new JSONObject(inJson);
				Snippet s;
				int snippetId = new Integer(inForm.getString("snippetId")).intValue();

					//OK, now get the object by ID
				s = ao.get(Snippet.class, snippetId);
				ao.delete(s);

	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save forms config" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"FAIL\",message:\""+e.getMessage()+"\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else if(iwfAction.equals("setConfig"))
    	{
    		try
    		{
                plog.info("IJF beginning set config...");
    			//backup the existing configuration first
    			StringBuffer cConfig = new StringBuffer();
    			cConfig.append("{\"status\":\"OK\",\"resultSet\":[");
        		for (FormSet fs : ao.find(FormSet.class))
                {
        			cConfig.append(getAoFormSetJson(fs));
                }
				cConfig.append("{}],\"customTypes\":[");
				for (CustomType ct : ao.find(CustomType.class))
				{
					cConfig.append(getCustomTypeJson(ct));
				}
				cConfig.append("{}]}");

        		Version v = ao.create(Version.class);
        		v.setDate(new Date());
        		v.setAuthor(userManager.getRemoteUsername(req));
        		v.setConfig(cConfig.toString());
        		v.save();
        		cleanVersions(50);


                plog.info("IJF clearing config...");

    			//clear current configuration
    			boolean loaded = false;
        		for (FormSet fs : ao.find(FormSet.class))
                {
        			for(Form f : fs.getForms())
        			{
        				ao.delete(f);
        			}
        			for(Snippet s : fs.getSnippets())
        			{
        				ao.delete(s);
        			}
                    ao.delete(fs);
                }

                for (CustomType ct : ao.find(CustomType.class))
				{
					ao.delete(ct);
				}



               plog.info("IJF prepping load...");
                //ijfConfig ie EITHER an array in which case it is the formSets, or an Object, which has two arrays:
                //  formSets and customTypes
	    		JSONObject jo = new JSONObject(inJson);

	    		//tests...if not null, this the formSets..
	    		JSONArray rs = jo.optJSONArray("ijfConfig");
	    		JSONArray cts = null;
	    		if(rs==null)
	    		{
					//jo is a level down
					JSONObject combinedConfig = jo.getJSONObject("ijfConfig");
					rs = combinedConfig.optJSONArray("formSets");
					cts = combinedConfig.optJSONArray("customTypes");
				}

	    		//JSONArray rs = jo.getJSONArray("ijfConfig");
               plog.info("IJF loading config forms...");

	    		JSONObject jsonFs;
	    		JSONArray jsonForms;
	    		JSONObject jsonForm;
	    		JSONObject jsonSnippet;
	    		for(int i = 0; i<rs.length();i++)
	    		{
	    			jsonFs = rs.getJSONObject(i);
	    			//this is one form set...
	        		if(!jsonFs.has("name")) break;
	        		final FormSet fs  = ao.create(FormSet.class);
	        		fs.setName(jsonFs.getString("name"));
	        		fs.setSettings(jsonFs.getString("settings"));
	        		fs.setProjectName(jsonFs.getString("projectName"));
	        		fs.setProjectId(jsonFs.getString("projectId"));

	        		fs.setCustomerKey("");
					fs.setIftFormGroup(jsonFs.getString("iftFormGroup"));
					fs.setIftFormGroupVersion(jsonFs.getString("iftFormGroupVersion"));

	        		fs.save();

		    		jsonForms = jsonFs.getJSONArray("forms");
		    		for(int k = 0; k<jsonForms.length();k++)
		    		{
		    			jsonForm = jsonForms.getJSONObject(k);
		    			if(!jsonForm.has("name")) break;
		        		Form frm = ao.create(Form.class);
		        		frm.setFormSet(fs);
		        		frm.setName(jsonForm.getString("name"));
		        		frm.setTestIssue(jsonForm.getString("testIssue"));
		        		if(jsonForm.has("formAnon")) frm.setFormAnon(jsonForm.getString("formAnon"));
		        		if(jsonForm.has("formProxy")) frm.setFormProxy(jsonForm.getString("formProxy"));
		        		frm.setIssueType(jsonForm.getString("issueType"));
		        		frm.setFormType(jsonForm.getString("formType"));
		        		frm.setFields(jsonForm.getString("fields"));
		        		frm.setSettings(jsonForm.getString("formSettings"));
		        		frm.save();
		    		}

		    		jsonForms = jsonFs.getJSONArray("snippets");
		    		for(int k = 0; k<jsonForms.length();k++)
		    		{
		    			jsonSnippet = jsonForms.getJSONObject(k);
		    			if(!jsonSnippet.has("name")) break;
		    			Snippet s = ao.create(Snippet.class);
		    			s.setFormSet(fs);
		    			s.setName(jsonSnippet.getString("name"));
		    			s.setSnippet(jsonSnippet.getString("snippet"));
		        		s.save();
		    		}
	    		}

             plog.info("IJF loading config types...");

				//handle custom types....
	    		//custom type configuration
	    		if(cts!=null)
	    		{
					for(int i = 0; i<cts.length();i++)
	    			{
						jsonFs = cts.getJSONObject(i);
						if(!jsonFs.has("name")) break;
						final CustomType ct  = ao.create(CustomType.class);
								ct.setName(jsonFs.getString("name"));
								ct.setDescription(jsonFs.getString("description"));
								ct.setCustomType(jsonFs.getString("customType"));
								ct.setFieldName(jsonFs.getString("fieldName"));
								ct.setSettings(jsonFs.getString("settings"));
	        		    ct.save();
					}
				}

	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;

    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save forms config see error" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{status:\"FAIL\"}");
	    		w.close();
	    		return;

    		}
    	}
    	else if(iwfAction.equals("setGroupConfig"))
    	{
    		try
    		{

    			//backup the existing configuration first
    			StringBuffer cConfig = new StringBuffer();
    			cConfig.append("{\"status\":\"OK\",\"resultSet\":[");
        		for (FormSet fs : ao.find(FormSet.class))
                {
        			cConfig.append(getAoFormSetJson(fs));
                }
				cConfig.append("{}],\"customTypes\":[");
				for (CustomType ct : ao.find(CustomType.class))
				{
					cConfig.append(getCustomTypeJson(ct));
				}
				cConfig.append("{}]}");

        		Version v = ao.create(Version.class);
        		v.setDate(new Date());
        		v.setAuthor(userManager.getRemoteUsername(req));
        		v.setConfig(cConfig.toString());
        		v.save();
        		cleanVersions(50);

        		///todo:  add clear function to remove versions > some count...

				//there should only be one new fg, and it it exists we delete, and
				//the form names within the group must not exist in the existing forms...
	    		JSONObject jo = new JSONObject(inJson);
	    		JSONArray rs = jo.getJSONArray("ijfConfig");
				JSONObject jsonFs;
				JSONArray jsonForms;
				JSONObject jsonForm;
	    		JSONObject jsonSnippet;
	    		if(rs.length()>1)
	    		{
					//error there is more than one form set in the load...stop...
					final PrintWriter w = res.getWriter();
						    		w.printf("{status:\"Failed, there is more than one form group in the file.\"}");
						    		w.close();
	    		    return;
				}

				jsonFs = rs.getJSONObject(0);
				String fsName = jsonFs.getString("name");
				Map<String, String> fNames = Maps.newHashMap();
				//for each form, look to see if name is being used by all forms other than those owned by this guy
				for (Form f : ao.find(Form.class))
				{
					if(f.getName()==null) continue;
					String tfsName = f.getFormSet().getName();
					if(tfsName.equals(fsName))
					{
						continue;
					}
					fNames.put(f.getName(),f.getName());
				}
				//rip through new names, make sure not there.
	    		jsonForms = jsonFs.getJSONArray("forms");
	    		for(int k = 0; k<jsonForms.length();k++)
	    		{
    				jsonForm = jsonForms.getJSONObject(k);
	    			if(fNames.containsKey(jsonForm.getString("name"))==true)
	    			{
		    			//bail because duplicate form name
						final PrintWriter w = res.getWriter();
			    		w.printf("{status:\"Failed, your file contains a form name already in use: "+ jsonForm.getString("name") +"\"}");
			    		w.close();
			    		return;
	    			}
	    		}

	    		//if we are here, then look for existing FS and remove it...
        		for (FormSet fs : ao.find(FormSet.class))
                {
        			if(fs.getName()==null) continue;
        			if(fs.getName().equals(fsName))
        			{
	        			for(Form f : fs.getForms())
	        			{
	        				ao.delete(f);
	        			}
	        			for(Snippet s : fs.getSnippets())
	        			{
	        				ao.delete(s);
	        			}
	                    ao.delete(fs);
        			}
                }

        		//load as normal....
    			boolean loaded = false;

	    		for(int i = 0; i<rs.length();i++)
	    		{
	    			jsonFs = rs.getJSONObject(i);
	    			//this is one form set...
	        		if(!jsonFs.has("name")) break;
	        		final FormSet fs  = ao.create(FormSet.class);
	        		fs.setName(jsonFs.getString("name"));
	        		fs.setSettings(jsonFs.getString("settings"));
	        		fs.setProjectName(jsonFs.getString("projectName"));
	        		fs.setProjectId(jsonFs.getString("projectId"));

					fs.setCustomerKey("");
					fs.setIftFormGroup(jsonFs.getString("iftFormGroup"));
					fs.setIftFormGroupVersion(jsonFs.getString("iftFormGroupVersion"));

	        		fs.save();

		    		jsonForms = jsonFs.getJSONArray("forms");
		    		for(int k = 0; k<jsonForms.length();k++)
		    		{
		    			jsonForm = jsonForms.getJSONObject(k);
		    			if(!jsonForm.has("name")) break;
		        		Form frm = ao.create(Form.class);
		        		frm.setFormSet(fs);
		        		frm.setName(jsonForm.getString("name"));
		        		frm.setTestIssue(jsonForm.getString("testIssue"));
		        		if(jsonForm.has("formAnon")) frm.setFormAnon(jsonForm.getString("formAnon"));
		        		if(jsonForm.has("formProxy")) frm.setFormProxy(jsonForm.getString("formProxy"));
		        		frm.setIssueType(jsonForm.getString("issueType"));
		        		frm.setFormType(jsonForm.getString("formType"));
		        		frm.setFields(jsonForm.getString("fields"));
		        		frm.setSettings(jsonForm.getString("formSettings"));
		        		frm.save();
		    		}

		    		jsonForms = jsonFs.getJSONArray("snippets");
		    		for(int k = 0; k<jsonForms.length();k++)
		    		{
		    			jsonSnippet = jsonForms.getJSONObject(k);
		    			if(!jsonSnippet.has("name")) break;
		    			Snippet s = ao.create(Snippet.class);
		    			s.setFormSet(fs);
		    			s.setName(jsonSnippet.getString("name"));
		    			s.setSnippet(jsonSnippet.getString("snippet"));
		        		s.save();
		    		}
	    		}
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{\"status\":\"OK\"}");
	    		w.close();
	    		return;
    		}
    		catch(JSONException e)
    		{
    			//failed json read
    			plog.error("Failed to save forms config see error" + e.getMessage());
	    		final PrintWriter w = res.getWriter();
	    		w.printf("{status:\"FAIL\"}");
	    		w.close();
	    		return;
    		}
    	}
    	else
    	{
    		final PrintWriter w = res.getWriter();
    		w.printf("{status:\"NA\"}");
    		w.close();
    		return;

    	}
    }

	private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
	}
	private URI getUri(HttpServletRequest request)
	{
		StringBuffer builder = request.getRequestURL();
		if (request.getQueryString() != null)
		{
			builder.append("?");
			builder.append(request.getQueryString());
		}
		return URI.create(builder.toString());
	}

	// This is what your MyPluginServlet.java should look like in its final stages.

}