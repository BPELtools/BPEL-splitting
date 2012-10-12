package org.bpel4chor.splitprocess.partition.util;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.Process;

public class BPELNamespaceContext implements NamespaceContext {

	private String BPELNS;

	private BPELNamespaceContext() {}

	public BPELNamespaceContext(Process process) {
		BPELNS = process.getElement().getNamespaceURI();
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (prefix.equalsIgnoreCase("bpel"))
			return BPELNS;
		else 
			return null;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		throw new IllegalStateException();
	}
	
	@Override
	public Iterator getPrefixes(String namespaceURI) {
		throw new IllegalStateException();
	}



}

