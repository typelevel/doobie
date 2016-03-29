package com.informix.jdbc;

import java.sql.Connection;

public class IfxSmartBlob {

	private static IfxSmartBlob mock;

	public IfxSmartBlob(Connection conn) {
	}

	public int IfxLoRead(int lofd, byte[] buf, int nbytes) { return mock.IfxLoRead(lofd, buf, nbytes); }

	public static void setMock(IfxSmartBlob blobMock) {
		mock = blobMock;
	}
}

