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
 */

package org.apache.hop.pipeline.transforms.xml.getxmldata;

import org.apache.hop.core.IProgressMonitor;
import org.apache.hop.core.IRunnableWithProgress;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.core.xml.XmlParserFactoryProducer;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.ProgressMonitorDialog;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.eclipse.swt.widgets.Shell;

import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes care of displaying a dialog that will handle the wait while we're finding out loop nodes
 * for an XML file
 */
public class LoopNodesImportProgressDialog {
  private static final Class<?> PKG = GetXmlDataMeta.class; // For Translator

  private Shell shell;

  private String[] xpaths;

  private String filename;
  private String xml;
  private String url;
  private String encoding;

  private ArrayList<String> listpath;

  private int nr;

  private PdOption option;

  /**
   * Creates a new dialog that will handle the wait while we're finding out loop nodes for an XML
   * file
   */
  public LoopNodesImportProgressDialog(Shell shell, String xmlSource, PdOption option) {
    this.shell = shell;
    this.option = option;
    this.xpaths = null;

    if (option.isXmlSourceIsFile()) {
      this.filename = xmlSource;
      this.xml = null;
      this.url = null;
    } else if (option.isUseUrl()) {
      this.filename = null;
      this.xml = null;
      this.url = xmlSource;
    } else {
      this.filename = null;
      this.xml = xmlSource;
      this.url = null;
    }

    this.encoding = option.getEncoding();
    this.listpath = new ArrayList<>();
    this.nr = 0;
  }

  public String[] open() {
    IRunnableWithProgress op =
        monitor -> {
          try {
            xpaths = doScan(monitor);
          } catch (Exception e) {
            e.printStackTrace();
            throw new InvocationTargetException(
                e,
                BaseMessages.getString(
                    PKG,
                    "GetXMLDateLoopNodesImportProgressDialog.Exception.ErrorScanningFile",
                    filename,
                    e.toString()));
          }
        };

    try {
      ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
      pmd.run(true, op);
    } catch (InvocationTargetException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(
              PKG, "GetXMLDateLoopNodesImportProgressDialog.ErrorScanningFile.Title"),
          BaseMessages.getString(
              PKG, "GetXMLDateLoopNodesImportProgressDialog.ErrorScanningFile.Message"),
          e);
    } catch (InterruptedException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(
              PKG, "GetXMLDateLoopNodesImportProgressDialog.ErrorScanningFile.Title"),
          BaseMessages.getString(
              PKG, "GetXMLDateLoopNodesImportProgressDialog.ErrorScanningFile.Message"),
          e);
    }

    return xpaths;
  }

  @SuppressWarnings("unchecked")
  private String[] doScan(IProgressMonitor monitor) throws Exception {
    monitor.beginTask(
        BaseMessages.getString(
            PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.ScanningFile", filename),
        1);

    SAXReader reader = XmlParserFactoryProducer.getSAXReader(null);
    monitor.worked(1);
    if (monitor.isCanceled()) {
      return null;
    }
    // Validate XML against specified schema?
    if (option.isValidating()) {
      reader.setValidation(true);
      reader.setFeature("http://apache.org/xml/features/validation/schema", true);
    } else {
      // Ignore DTD
      reader.setEntityResolver(new IgnoreDtdEntityResolver());
    }
    monitor.worked(1);
    monitor.beginTask(
        BaseMessages.getString(PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.ReadingDocument"),
        1);
    if (monitor.isCanceled()) {
      return null;
    }
    InputStream is = null;
    try {
      Document document = null;
      if (!Utils.isEmpty(filename)) {
        is = HopVfs.getInputStream(filename);
        document = reader.read(is, encoding);
      } else {
        if (!Utils.isEmpty(xml)) {
          document = reader.read(new StringReader(xml));
        } else {
          document = reader.read(new URL(url));
        }
      }
      monitor.worked(1);
      monitor.beginTask(
          BaseMessages.getString(
              PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.DocumentOpened"),
          1);
      monitor.worked(1);
      monitor.beginTask(
          BaseMessages.getString(PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.ReadingNode"),
          1);

      if (monitor.isCanceled()) {
        return null;
      }
      List<Node> nodes = document.selectNodes(document.getRootElement().getName());
      monitor.worked(1);
      monitor.subTask(
          BaseMessages.getString(PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.FetchNodes"));

      if (monitor.isCanceled()) {
        return null;
      }
      for (Node node : nodes) {
        if (monitor.isCanceled()) {
          return null;
        }
        if (!listpath.contains(node.getPath())) {
          nr++;
          monitor.subTask(
              BaseMessages.getString(
                  PKG,
                  "GetXMLDateLoopNodesImportProgressDialog.Task.FetchNodes",
                  String.valueOf(nr)));
          monitor.subTask(
              BaseMessages.getString(
                  PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.AddingNode", node.getPath()));
          listpath.add(node.getPath());
          addLoopXPath(node, monitor);
        }
      }
      monitor.worked(1);
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
        /* Ignore */
      }
    }
    String[] listXpath = listpath.toArray(new String[listpath.size()]);

    monitor.setTaskName(
        BaseMessages.getString(PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.NodesReturned"));

    monitor.done();

    return listXpath;
  }

  private void addLoopXPath(Node node, IProgressMonitor monitor) {
    Element ce = (Element) node;
    monitor.worked(1);
    // List child
    for (int j = 0; j < ce.nodeCount(); j++) {
      if (monitor.isCanceled()) {
        return;
      }
      Node cnode = ce.node(j);

      if (!Utils.isEmpty(cnode.getName())) {
        Element cce = (Element) cnode;
        if (!listpath.contains(cnode.getPath())) {
          nr++;
          monitor.subTask(
              BaseMessages.getString(
                  PKG,
                  "GetXMLDateLoopNodesImportProgressDialog.Task.FetchNodes",
                  String.valueOf(nr)));
          monitor.subTask(
              BaseMessages.getString(
                  PKG, "GetXMLDateLoopNodesImportProgressDialog.Task.AddingNode", cnode.getPath()));
          listpath.add(cnode.getPath());
        }
        // let's get child nodes
        if (cce.nodeCount() > 1) {
          addLoopXPath(cnode, monitor);
        }
      }
    }
  }

  public PdOption getOption() {
    return option;
  }
}
