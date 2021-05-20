package com.idealfed.worklogs.jiraforms.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;

@Preload
public interface Snippet extends Entity
{

	public FormSet getFormSet();
    public void setFormSet(FormSet formSet);

    String getName();
    void setName(String name);

    @StringLength(value=StringLength.UNLIMITED)
    String getSnippet();
    void setSnippet(String snippet);

    String getComment();
    void setComment(String comment);

}
