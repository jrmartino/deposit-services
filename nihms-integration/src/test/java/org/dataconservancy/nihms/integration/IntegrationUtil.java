/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.nihms.integration;

import com.google.common.net.InetAddresses;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class IntegrationUtil {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private String ftpHost;

    private int ftpPort;

    private FTPClient ftpClient;

    public IntegrationUtil(String ftpHost, int ftpPort, FTPClient ftpClient) {
        this.ftpHost = ftpHost;
        this.ftpPort = ftpPort;
        this.ftpClient = ftpClient;
    }

    public void assertPositiveReply() {
        int replyCode = ftpClient.getReplyCode();
        String replyString = ftpClient.getReplyString();
        assertTrue("(" + ftpClient() + ") Command failed: " + replyCode + " " + replyString,
                FTPReply.isPositiveCompletion(replyCode));
    }

    public void connect() throws IOException {
        LOG.debug("({}) Connecting to {}:{} ...", ftpClient(), ftpHost, ftpPort);

        long waitMs = 2000;
        long start = System.currentTimeMillis();
        boolean connectionSuccess = false;

        do {
            try {
                LOG.debug("({}) Attempting connection to {}:{} ...", ftpClient(), ftpHost, ftpPort);
                if (InetAddresses.isInetAddress(ftpHost)) {
                    ftpClient.connect(InetAddresses.forString(ftpHost), ftpPort);
                } else {
                    ftpClient.connect(ftpHost, ftpPort);
                }
                if (!ftpClient.sendNoOp()) {
                    LOG.debug("({}) NOOP *FAILED*, connection to {}:{} not established.", ftpClient(), ftpHost, ftpPort);
                    connectionSuccess = false;
                } else {
                    connectionSuccess = true;
                }
            } catch (FTPConnectionClosedException e) {
                // is there a bug in the Apache FTP library which attempts to re-use a socket that has been closed?
                // retry until a timeout is reached or a connection is successful.
                try {
                    LOG.debug("({}) Connection *FAILED* to {}:{}, sleeping for {} ms ...",
                            ftpClient(), ftpHost, ftpPort, waitMs);
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
        } while (!connectionSuccess && System.currentTimeMillis() - start < 30000);

        assertPositiveReply();
        LOG.debug("({}) Successfully connected to {}:{}", ftpClient(), ftpHost, ftpPort);
    }

    public void disconnect() throws IOException {
        if (ftpClient == null) {
            LOG.debug("({}) Not disconnecting because FTP client is null.", ftpClient());
        }

        if (!ftpClient.isConnected()) {
            LOG.debug("({}) Not disconnecting because the FTP client isn't connected.", ftpClient());
        }

        if (ftpClient != null && ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    public void login() throws IOException {
        assertTrue(ftpClient.login("nihmsftpuser", "nihmsftppass"));

        assertPositiveReply();
    }

    public void logout() throws IOException {
        assertTrue(ftpClient.logout());
        assertPositiveReply();
    }

    private String ftpClient() {
        if (ftpClient == null) {
            return "null";
        } else {
            return ftpClient.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(ftpClient));
        }
    }

}