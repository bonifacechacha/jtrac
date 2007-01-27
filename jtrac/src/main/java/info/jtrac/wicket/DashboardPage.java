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

import info.jtrac.domain.Counts;
import info.jtrac.domain.CountsHolder;
import info.jtrac.domain.Space;
import info.jtrac.domain.User;
import info.jtrac.domain.UserSpaceRole;
import info.jtrac.util.SecurityUtils;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;

/**
 * dashboard page
 */
public class DashboardPage extends BasePage {
    
    public DashboardPage() {
        
        super("Dashboard");
        setVersioned(false);
        
        add(new HeaderPanel(null));
        
        final User user = getJtrac().loadUser(SecurityUtils.getPrincipal().getId());
        final CountsHolder countsHolder = getJtrac().loadCountsForUser(user);        
        
        border.add(new ListView("dashboardRows", user.getSpaceRoles()) {
            protected void populateItem(final ListItem listItem) {
                UserSpaceRole usr = (UserSpaceRole) listItem.getModelObject();
                Counts counts = countsHolder.getCounts().get(usr.getSpace().getId());
                DashboardRowPanel dashboardRow = new DashboardRowPanel("dashboardRow", usr.getSpace(), counts, user);
                listItem.add(dashboardRow);
            }
        });
        
        DashboardTotalPanel panel = new DashboardTotalPanel("total", countsHolder);
        if(user.getSpaceRoles().size() == 1) {
            panel.setVisible(false);
        }
        border.add(panel);      
        
    }
    
}
