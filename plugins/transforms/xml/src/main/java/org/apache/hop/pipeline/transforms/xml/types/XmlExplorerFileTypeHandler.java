/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.hop.pipeline.transforms.xml.types;

import org.apache.hop.core.Props;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.hopgui.file.IHopFileTypeHandler;
import org.apache.hop.ui.hopgui.perspective.explorer.ExplorerFile;
import org.apache.hop.ui.hopgui.perspective.explorer.ExplorerPerspective;
import org.apache.hop.ui.hopgui.perspective.explorer.file.IExplorerFileTypeHandler;
import org.apache.hop.ui.hopgui.perspective.explorer.file.types.base.BaseExplorerFileTypeHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * How do we handle an SVG file in file explorer perspective?
 */
public class XmlExplorerFileTypeHandler extends BaseExplorerFileTypeHandler implements IExplorerFileTypeHandler {

  public XmlExplorerFileTypeHandler( HopGui hopGui, ExplorerPerspective perspective, ExplorerFile explorerFile ) {
    super( hopGui, perspective, explorerFile );
  }

  @Override
  public void renderFile(Composite composite) {
    // Render the file by simply showing the XML content as a text widget...
    //
    Text wXml = new Text(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    PropsUi.getInstance().setLook(wXml, Props.WIDGET_STYLE_FIXED);
    wXml.setEditable(false);
    FormData fdXml = new FormData();
    fdXml.left = new FormAttachment(0, 0);
    fdXml.right = new FormAttachment(100, 0);
    fdXml.top = new FormAttachment(0, 0);
    fdXml.bottom = new FormAttachment(100, 0);
    wXml.setLayoutData(fdXml);

    // TODO: add bottom section to show status, size, cursor position...
    //

    // Load the content of the XML file...
    //
    File file = new File(explorerFile.getFilename());
    if (file.exists()) {
      try {
        String contents = new String( Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        wXml.setText(contents);
      } catch (Exception e) {
        LogChannel.UI.logError(
          "Error reading contents of file '" + explorerFile.getFilename() + "'", e);
      }
    }
  }

}
