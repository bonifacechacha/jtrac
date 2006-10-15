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

package info.jtrac.webflow;

import org.springframework.webflow.RequestContext;
import org.springframework.webflow.State;
import org.springframework.webflow.execution.ConditionalFlowExecutionListenerLoader;
import org.springframework.webflow.execution.FlowExecutionListenerAdapter;

/**
 * Spring WebFlow listener loader
 */
public class FlowExecutionListenerLoader extends ConditionalFlowExecutionListenerLoader {

    public FlowExecutionListenerLoader() {
        addListener(new JtracFlowExecutionListener());
    }

    class JtracFlowExecutionListener extends FlowExecutionListenerAdapter {

        @Override
        public void stateEntered(RequestContext context, State prevState, State newState) {
            logger.debug("prevState: " + prevState);
            logger.debug("newState: " + newState);
        }

    }
}
