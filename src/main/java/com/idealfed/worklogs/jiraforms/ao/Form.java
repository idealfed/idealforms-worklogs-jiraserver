package com.idealfed.worklogs.jiraforms.ao;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;

@Preload
public interface Form extends Entity
{
	public FormSet getFormSet();
    public void setFormSet(FormSet formSet);

    String getName();
    void setName(String name);

    String getTestIssue();
    void setTestIssue(String name);

    String getIssueType();
    void setIssueType(String name);

    String getFormAnon();
    void setFormAnon(String name);

    String getFormProxy();
    void setFormProxy(String name);

    String getFormType();
    void setFormType(String name);


    @StringLength(value=StringLength.UNLIMITED)
    String getFields();
    void setFields(String fields);

    @StringLength(value=StringLength.UNLIMITED)
    String getSettings();
    void setSettings(String settings);

	//@OneToMany
    //public FormSetting[] getFormSettings();

	//@OneToMany
	//public Field[] getFields();
}
