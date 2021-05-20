package com.idealfed.worklogs.poc.craft;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.json.JSONEscaper;
import com.idealfed.worklogs.jiraforms.ao.*;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class LifeCyleManager implements LifecycleAware
{
    //static private final String PLUGIN_KEY="com.idealfed.utils.idealworklogs";

    protected final ActiveObjects ao;

    //protected final EventPublisher eventPublisher;

    private static final Logger log = LogManager.getLogger("atlassian.plugin");


    public LifeCyleManager(final ActiveObjects activeObjects)
    {
        this.ao = activeObjects;
        //this.eventPublisher = eventPublisher;
    }


    public void onStart() {
	   try
       {
		   log.info("LifeCyleManager lifecyle started"); //, pausing for AO init...");
		   //TimeUnit.SECONDS.sleep(3);
		   //log.info("LifeCyleManager lifecyle continuing...");

       //reset to the form config included in the package..
       //location = xxxx
       //clear AO...

		    int ctr = 0;
			for (FormSet fs : ao.find(FormSet.class))
			{
				for(Form f : fs.getForms())
				{
					ctr++; //
					ao.delete(f);
				}
				for(Snippet s : fs.getSnippets())
				{
					ctr++; //
					ao.delete(s);
				}
				   ctr++; //
				   ao.delete(fs);
			}
			for (CustomType ct : ao.find(CustomType.class))
			{
				ctr++; //
				ao.delete(ct);
			}
			log.info("LifeCyleManager cleared IFT configuration, cleared objects: " + String.valueOf(ctr));

			//in theory cleared

			InputStream in = getClass().getResourceAsStream("/iftProductConfig.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String inputLine;
			StringBuffer iftConfig = new StringBuffer();
			while ((inputLine = reader.readLine()) != null) {
				iftConfig.append(inputLine);
			}
			reader.close();

			//log.info("Config file is: " + iftConfig);

			//now parse config file and build configuration based upon...
		    log.info("IFT parsing resource config...");

		    String iftCftStr = iftConfig.toString();
		    iftCftStr= iftCftStr.replaceAll("%","~pct~");
			JSONObject combinedConfig = new JSONObject(iftCftStr);
			log.info("IFT configuration parsed...");// + combinedConfig.toString());

			JSONArray rs = null;
			JSONArray cts = null;

			rs = combinedConfig.optJSONArray("formSets");
			cts = combinedConfig.optJSONArray("customTypes");

    		log.info("IFT form sets and custom types ready, loading form groups...");

			JSONObject jsonFs;
			JSONArray jsonForms;
			JSONObject jsonForm;
			JSONObject jsonSnippet;
			if(rs!=null)
			{
				for(int i = 0; i<rs.length();i++)
				{
					jsonFs = rs.getJSONObject(i);
					//this is one form set...
					if(!jsonFs.has("name")) break;

					log.info("IFT working formgroup " + jsonFs.getString("name") + " index:" +  String.valueOf(i));

					final FormSet fs  = ao.create(FormSet.class);
					fs.setName(jsonFs.getString("name"));
					fs.setSettings(jsonFs.getString("settings"));
					fs.setProjectName(jsonFs.getString("projectName"));
					fs.setProjectId(jsonFs.getString("projectId"));
					fs.save();

					jsonForms = jsonFs.getJSONArray("forms");
					for(int k = 0; k<jsonForms.length();k++)
					{
						jsonForm = jsonForms.getJSONObject(k);
						if(!jsonForm.has("name")) break;

						log.info("IFT working form " + jsonForm.getString("name") + " index:" +  String.valueOf(k));

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

						log.info("IFT working snippet " + jsonSnippet.getString("name") + " index:" +  String.valueOf(k));

						Snippet s = ao.create(Snippet.class);
						s.setFormSet(fs);
						s.setName(jsonSnippet.getString("name"));
						s.setSnippet(jsonSnippet.getString("snippet"));
						s.save();
					}
				}

			}

			log.info("IFT loading config types...");

			//handle custom types....
			//custom type configuration
			if(cts!=null)
			{
				for(int i = 0; i<cts.length();i++)
				{
					jsonFs = cts.getJSONObject(i);
					if(!jsonFs.has("name")) break;

					log.info("IFT working custom type " + jsonFs.getString("name") + " index:" +  String.valueOf(i));

					final CustomType ct  = ao.create(CustomType.class);
							ct.setName(jsonFs.getString("name"));
							ct.setDescription(jsonFs.getString("description"));
							ct.setCustomType(jsonFs.getString("customType"));
							ct.setFieldName(jsonFs.getString("fieldName"));
							ct.setSettings(jsonFs.getString("settings"));
					ct.save();
				}
			}

			log.info("IFT loaded using resource file, end.");


   		}
   		catch(Exception e)
   		{
			log.info("LifeCyleManager error",e);
		}
    }

    public void onStop() {
       log.info("LifeCyleManager lifecyle stopped");
    }


}