package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.api.input.FileSync;
import com.hello.suripu.core.db.FileManifestDAO;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/files")
public class FileResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileResources.class);

    private final FileManifestDAO fileManifestDAO;

    public FileResources(final FileManifestDAO fileManifestDAO) {
        this.fileManifestDAO = fileManifestDAO;
    }

    @ScopesAllowed({OAuthScope.ADMINISTRATION_READ})
    @GET
    @Path("/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listManifestFiles(@Auth final AccessToken accessToken,
                          @PathParam("sense_id") final String senseId) {

        final List<String> sounds = Lists.newArrayList();
        final Optional<FileSync.FileManifest> manifestOptional = fileManifestDAO.getManifest(senseId);

        if (!manifestOptional.isPresent()) {
            LOGGER.warn("dao=fileManifestDAO method=getManifest sense-id={} error=not-found", senseId);
            return sounds;
        }

        final FileSync.FileManifest manifest = manifestOptional.get();
        for(FileSync.FileManifest.File file : manifest.getFileInfoList()) {
            final String path = Joiner.on("/").join(
                    file.getDownloadInfo().getSdCardPath(),
                    file.getDownloadInfo().getSdCardFilename()
            );
            sounds.add(path);
        }
        LOGGER.info("action=list-file-manifest sense-id={} num_files={}", senseId, sounds.size());
        return sounds;
    }
}
