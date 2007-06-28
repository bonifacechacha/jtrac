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

package info.jtrac.wicket.search;

import info.jtrac.domain.ColumnHeading;
import info.jtrac.domain.FilterCriteria;
import info.jtrac.domain.FilterCriteria.Expression;
import info.jtrac.domain.User;
import info.jtrac.wicket.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class FilterPage extends BasePage {
    
    Map<String, FilterCriteria> map = new LinkedHashMap<String, FilterCriteria>();
    
    FilterCriteria filterCriteria = new FilterCriteria(null);
    
    List<Expression> expressionChoices;            
    
    public FilterPage() {
        final Form form = new Form("form");        
        add(form);
        form.setModel(new CompoundPropertyModel(filterCriteria));       
        // column ==============================================================
        List<ColumnHeading> columnHeadings = ColumnHeading.getColumnHeadings(getCurrentSpace(), this);        
        DropDownChoice columnChoice = new DropDownChoice("columnHeading", columnHeadings, new IChoiceRenderer() {
            public Object getDisplayValue(Object o) {
                return ((ColumnHeading) o).getLabel();
            }
            public String getIdValue(Object o, int i) {
                return ((ColumnHeading) o).getName();
            }
        });        
        form.add(columnChoice);
        filterCriteria.setColumnHeading(columnHeadings.get(0));
        // values ==============================================================        
        final Fragment frag = initChoices();
        form.add(frag);
        // expression ==========================================================
        final DropDownChoice expressionChoice = new DropDownChoice("expression", expressionChoices, new IChoiceRenderer() {
            public Object getDisplayValue(Object o) {
                String key = ((Expression) o).getKey();
                return localize("item_filter." + key);
            }
            public String getIdValue(Object o, int i) {
                return ((Expression) o).getKey();
            }            
        });
        expressionChoice.setOutputMarkupId(true);
        form.add(expressionChoice);        
        // ajax ================================================================
        columnChoice.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            protected void onUpdate(AjaxRequestTarget target) {
                ColumnHeading ch = (ColumnHeading) getFormComponent().getConvertedInput();
                filterCriteria.setColumnHeading(ch);
                Fragment temp = initChoices();
                form.replace(temp);
                expressionChoice.setChoices(expressionChoices);
                target.addComponent(expressionChoice);                
                target.addComponent(temp);
            }
        });        
        // list ================================================================
        final AjaxListView listView = new AjaxListView("filters");
        form.add(new AjaxButton("add") {
            protected void onSubmit(AjaxRequestTarget target, Form unused) {
                if(filterCriteria.getColumnHeading() == null) {
                    return;
                }
                Item newItem = listView.addItem();                
                target.prependJavascript("var myTr = document.createElement('tr');"                        
                        + " myTr.id = '" + newItem.getMarkupId() + "';"
                        + " document.getElementById('container').appendChild(myTr);");                
                target.addComponent(newItem);                
            }            
        });
        form.add(listView);
    }
        
    public Fragment initChoices() {
        expressionChoices = new ArrayList<Expression>();
        ColumnHeading ch = filterCriteria.getColumnHeading();
        Fragment fragment = null;
        if(ch.isField()) {
            switch(ch.getField().getName().getType()) {
                case 1:
                case 2:
                case 3:
                    expressionChoices.add(Expression.IN);
                    expressionChoices.add(Expression.NOT_IN);
                    fragment = new Fragment("fragParent", "multiSelect");
                    final Map<String, String> options = ch.getField().getOptions();
                    fragment.add(new JtracCheckBoxMultipleChoice("values", new ArrayList(options.keySet()), new IChoiceRenderer() {
                        public Object getDisplayValue(Object o) {
                            return options.get(o);
                        }
                        public String getIdValue(Object o, int i) {
                            return o.toString();
                        }
                    }));
                    break; // drop down list
                case 4: // decimal number
                    expressionChoices.add(Expression.EQ);
                    expressionChoices.add(Expression.NOT_EQ);
                    expressionChoices.add(Expression.GE);
                    expressionChoices.add(Expression.LE);
                    fragment = new Fragment("fragParent", "textField");
                    fragment.add(new TextField("value"));
                    break;
                case 6: // date
                    expressionChoices.add(Expression.GE);
                    expressionChoices.add(Expression.LE);
                    fragment = new Fragment("fragParent", "textField");
                    fragment.add(new TextField("value"));                    
                    break;
                case 5: // free text
                    expressionChoices.add(Expression.CONTAINS);
                    fragment = new Fragment("fragParent", "textField");
                    fragment.add(new TextField("value"));                    
                    break;
                default: 
                    throw new RuntimeException("Unknown Column Heading " + ch.getName());
            }
        } else {
            if(ch.getName().equals(ColumnHeading.STATUS)) {
                expressionChoices.add(Expression.IN);
                expressionChoices.add(Expression.NOT_IN);
                fragment = new Fragment("fragParent", "multiSelect");
                final Map<Integer, String> options = getCurrentSpace().getMetadata().getStates();
                fragment.add(new JtracCheckBoxMultipleChoice("values", new ArrayList(options.keySet()), new IChoiceRenderer() {
                    public Object getDisplayValue(Object o) {
                        return options.get(o);
                    }
                    public String getIdValue(Object o, int i) {
                        return o.toString();
                    }
                }));                
            } else if(ch.getName().equals(ColumnHeading.ASSIGNED_TO) || ch.getName().equals(ColumnHeading.LOGGED_BY)) {
                expressionChoices.add(Expression.IN);
                expressionChoices.add(Expression.NOT_IN);
                fragment = new Fragment("fragParent", "multiSelect");
                List<User> users = getJtrac().findUsersForSpace(getCurrentSpace().getId());
                fragment.add(new JtracCheckBoxMultipleChoice("values", users, new IChoiceRenderer() {
                    public Object getDisplayValue(Object o) {
                        return ((User) o).getName();
                    }
                    public String getIdValue(Object o, int i) {
                        return ((User) o).getId() + "";
                    }
                }));                
            } else if(ch.getName().equals(ColumnHeading.TIME_STAMP)) {
                expressionChoices.add(Expression.GE);
                expressionChoices.add(Expression.LE);
                fragment = new Fragment("fragParent", "textField");
                fragment.add(new TextField("value"));                 
            } else {
                throw new RuntimeException("Unknown Column Heading " + ch.getName());
            }                        
        }
        filterCriteria.setExpression(expressionChoices.get(0)); 
        fragment.setOutputMarkupId(true);
        return fragment;
    }
    
    public class AjaxListView extends RefreshingView {                
        
        public AjaxListView(String id) {
            super(id);            
        }
        
        public Item addItem() {
            String uniqueId = newChildId();
            Item item = newItem(uniqueId, map.size(), new Model(uniqueId));
            map.put(uniqueId, new FilterCriteria(filterCriteria.getColumnHeading()));
            populateItem(item);
            add(item);
            return item;
        }
        
        public void removeItem(Item item) {
            map.remove(item.getModelObject());            
        }
        
        protected Iterator getItemModels() {
            List<IModel> models = new ArrayList<IModel>();
            for(String s : map.keySet()) {
                models.add(new Model(s));
            }
            return models.iterator();
        }
        
        protected void populateItem(final Item item) {
            FilterCriteria filterCriteria = map.get(item.getModelObject());
            item.add(new Label("columnHeading", filterCriteria.getColumnHeading().getLabel()));
            item.add((new AjaxButton("remove") {
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    AjaxListView.this.removeItem(item);                
                    target.appendJavascript("var myTr = document.getElementById('" + item.getMarkupId() + "');"
                            + " myTr.parentNode.removeChild(myTr);");
                }
            })); 
            item.setOutputMarkupId(true);
        }        
    }
    
}
