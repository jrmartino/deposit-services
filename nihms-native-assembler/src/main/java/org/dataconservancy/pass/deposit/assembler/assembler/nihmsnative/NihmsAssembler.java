/*
 * Copyright 2018 Johns Hopkins University
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

package org.dataconservancy.pass.deposit.assembler.assembler.nihmsnative;

import org.dataconservancy.pass.deposit.assembler.MetadataBuilder;
import org.dataconservancy.pass.deposit.assembler.PackageStream;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.dataconservancy.pass.deposit.assembler.shared.AbstractAssembler;
import org.dataconservancy.pass.deposit.assembler.shared.DepositFileResource;
import org.dataconservancy.pass.deposit.assembler.shared.Extension;
import org.dataconservancy.pass.deposit.assembler.shared.MetadataBuilderFactory;
import org.dataconservancy.pass.deposit.assembler.shared.ResourceBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class NihmsAssembler extends AbstractAssembler {

    /**
     * Package specification URI identifying the NIHMS native packaging spec, as specified by their 07/2017
     * bulk publishing pdf.
     */
    public static final String SPEC_NIHMS_NATIVE_2017_07 = "nihms-native-2017-07";

    /**
     * Mime type of zip files.
     */
    public static final String APPLICATION_GZIP = "application/gzip";

    private static final String PACKAGE_FILE_NAME = "%s_%s_%s";

    @Autowired
    public NihmsAssembler(MetadataBuilderFactory mbf, ResourceBuilderFactory rbf) {
        super(mbf, rbf);
    }

    @Override
    protected PackageStream createPackageStream(DepositSubmission submission,
                                                List<DepositFileResource> custodialResources, MetadataBuilder mb,
                                                ResourceBuilderFactory rbf) {
        mb.spec(SPEC_NIHMS_NATIVE_2017_07);
        mb.archive(PackageStream.ARCHIVE.TAR);
        mb.archived(true);
        mb.compressed(true);
        mb.compression(PackageStream.COMPRESSION.GZIP);
        mb.mimeType(APPLICATION_GZIP);

        namePackage(submission, mb);

        NihmsZippedPackageStream stream = new NihmsZippedPackageStream(submission, custodialResources, mb, rbf);
        stream.setManifestSerializer(new NihmsManifestSerializer(submission.getManifest()));
        stream.setMetadataSerializer(new NihmsMetadataSerializer(submission.getMetadata()));
        return stream;
    }

    static void namePackage(DepositSubmission submission, MetadataBuilder mb) {
        String submissionUuid = null;

        try {
            URI submissionUri = URI.create(submission.getId());
            submissionUuid = submissionUri.getPath().substring(submissionUri.getPath().lastIndexOf("/") + 1);
        } catch (Exception e) {
            submissionUuid = UUID.randomUUID().toString();
        }

        String packageFileName = String.format(PACKAGE_FILE_NAME,
                SPEC_NIHMS_NATIVE_2017_07,
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-MM-ss")),
                submissionUuid);

        StringBuilder ext = new StringBuilder(packageFileName);
        PackageStream.Metadata md = mb.build();
        if (md.archived()) {
            switch (md.archive()) {
                case TAR:
                    ext.append(".").append(Extension.TAR.getExt());
                    break;
                case ZIP:
                    ext.append(".").append(Extension.ZIP.getExt());
                    break;
            }
        }

        if (md.compressed()) {
            switch (md.compression()) {
                case BZIP2:
                    ext.append(".").append(Extension.BZ2.getExt());
                    break;
                case GZIP:
                    ext.append(".").append(Extension.GZ.getExt());
                    break;
            }
        }

        mb.name(sanitizeFilename(ext.toString()));
    }

}
