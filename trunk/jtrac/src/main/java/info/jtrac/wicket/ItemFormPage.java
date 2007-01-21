  /*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.jtrac.wicket;

import info.jtrac.domain.Field;
import info.jtrac.domain.Item;
import java.util.Arrays;
import java.util.List;
import wicket.markup.html.basic.Label;
import wicket.markup.html.form.DropDownChoice;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.TextArea;
import wicket.markup.html.form.TextField;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.html.panel.Fragment;
import wicket.model.BoundCompoundPropertyModel;

/**
 * Create / Edit item form page
 */
public class ItemFormPage extends BasePage {
    
    public ItemFormPage(Item item) {
        super("Edit Item");
        border.add(new FeedbackPanel("feedback"));        
        border.add(new ItemForm("form", item));        
    }
    
    private class ItemForm extends Form {
        
        public ItemForm(String id, Item item) {
            super(id);
            final BoundCompoundPropertyModel model = new BoundCompoundPropertyModel(item);
            setModel(model);
            add(new TextField("summary").setRequired(true).add(new ErrorHighlighter()));
            add(new TextArea("detail").setRequired(true).add(new ErrorHighlighter()));
            List<Field> fields = item.getSpace().getMetadata().getFieldList();
            add(new ListView("fields", fields) {
                protected void populateItem(ListItem listItem) {
                    Field field = (Field) listItem.getModelObject();
                    listItem.add(new Label("label", field.getLabel()));
                    listItem.add(new Label("star", "*"));
                    if (field.getName().getType() < 4) {
                        Fragment f = new Fragment("field", "select");
                        List<String> list = Arrays.asList(new String[] { "foo", "bar", "baz" });
                        f.add(model.bind(new DropDownChoice("select", list), field.getNameText()));
                        listItem.add(f);
                    }
                }
            });
        }
        
        @Override
        protected void onSubmit() {
            info("the form was submitted");
            Item item = (Item) getModelObject();
            info("summary: " + item.getSummary());
            info("detail: " + item.getDetail());
        }
        
    }
    
}
