/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages {
	private static final String BUNDLE_NAME = "org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow.messages"; //$NON-NLS-1$

	public static String CriticalFlowView_multipleStates;

	public static String ControlFlowView_stateTypeName;

	public static String CriticalFlowView_processColumn;
	
	static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
