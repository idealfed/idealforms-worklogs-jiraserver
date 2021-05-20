package com.idealfed.worklogs.jiraforms.ao;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;

@Preload
public interface Version extends Entity
{

	Date getDate();
	void setDate(Date inDate);

	String getAuthor();
	void setAuthor(String inUser);

    @StringLength(value=StringLength.UNLIMITED)
    String getConfig();
    void setConfig(String fields);


}
