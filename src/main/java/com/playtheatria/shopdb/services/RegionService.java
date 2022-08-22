package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.Player;
import com.playtheatria.shopdb.database.Region;
import com.playtheatria.shopdb.models.chestshops.Location;
import com.playtheatria.shopdb.models.chestshops.Server;
import com.playtheatria.shopdb.models.regions.Bounds;
import com.playtheatria.shopdb.models.regions.RegionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class RegionService {
    final Logger LOGGER = LoggerFactory.getLogger(RegionService.class);
    private final HashMap<String, Server> servers = new HashMap<>();
    private static final Pattern p = Pattern.compile("[a-zA-Z0-9+_]{3,16}");

    public RegionService() {
        this.servers.put("The_Ark", Server.THE_ARK);
    }

    public Region listRegion(RegionRequest request) {
        Region region = convert(request, true, null);
        region.persist();
        LOGGER.info("Successfully listed region " + request.getName());
        return region;
    }

    public Region unlistRegion(RegionRequest request) {
        Region region = convert(request, false, null);
        region.persist();
        LOGGER.info("Successfully unlisted region " + request.getName());
        return region;
    }

    public Region convert(RegionRequest request, Boolean active, HashMap<String, Player> players) {
        if (!regionRequestIsValid(request)) return null;

        Server server = servers.get(request.getServer());
        Bounds bounds = sort(request.getiBounds(), request.getoBounds());

        Region region = Region.findByServerAndName(server, request.getName());

        if (region == null) {
            region = new Region();
            region.active = Boolean.FALSE;
            region.name = request.getName().toLowerCase(Locale.ROOT);
            region.server = server;
            region.iBounds = bounds.getLowerBounds();
            region.oBounds = bounds.getUpperBounds();
        }

        if (active == Boolean.TRUE) {
            region.active = true;
        } else if (active == Boolean.FALSE) {
            region.active = false;
        }

        if (players != null) {
            List<Player> regionOwners = new ArrayList<>();
            for (String p : request.getMayorNames()) {
                regionOwners.add(players.get(p));
            }
            region.mayors = regionOwners;
        } else {
            if (request.getMayorNames().size() > 0) {
                region.mayors = new ArrayList<>(Player.getOrAddPlayers(request.getMayorNames()).values());
            } else {
                region.mayors = new ArrayList<>();
            }
        }

        region.lastUpdated = new Timestamp(System.currentTimeMillis());
        return region;
    }

    public HashMap<String, Region> upsertRegions(Set<RegionRequest> requests, HashMap<String, Player> players) {
        HashMap<String, Region> result = new HashMap<>();

        for (RegionRequest request : requests) {
            Region region = convert(request, null, players);
            if (region != null) {
                result.put(request.getName() + "|" + request.getServer(), region);
            }
        }

        Region.persist(result.values());
        return result;
    }

    public Region findActiveOrSmallest(List<Region> regions) {
        if (regions == null || regions.size() == 0) return null;

        long smallestSize = 0;
        int smallestIndex = 0;

        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);

            if (r.active) return r;

            long x = Math.abs(r.getiBounds().getX() - r.getoBounds().getX());
            long y = Math.abs(r.getiBounds().getY() - r.getoBounds().getY());
            long z = Math.abs(r.getiBounds().getZ() - r.getoBounds().getZ());
            long size = x * y * z;
            if (smallestSize == 0 || size < smallestSize) {
                smallestSize = size;
                smallestIndex = i;
            }
        }

        return regions.get(smallestIndex);
    }

    public boolean regionRequestIsValid(RegionRequest regionRequest) {
        if (regionRequest == null) {
            LOGGER.warn("Filtering out null region request.");
            return false;
        }

        if (regionRequest.getName() == null || regionRequest.getName().isEmpty()) {
            LOGGER.warn("Filtering out region request with invalid name: " + regionRequest);
            return false;
        }

        if (regionRequest.getServer() == null) {
            LOGGER.warn("Filtering out region request with null server: " + regionRequest);
            return false;
        }

        Server server = servers.get(regionRequest.getServer());
        if (server == null) {
            LOGGER.warn("Filtering out region request with invalid server: " + regionRequest);
            return false;
        }

        if (regionRequest.getiBounds() == null) {
            LOGGER.warn("Filtering out region request with invalid iBounds: " + regionRequest);
            return false;
        }

        if (regionRequest.getoBounds() == null) {
            LOGGER.warn("Filtering out region request with invalid oBounds: " + regionRequest);
            return false;
        }

        if (regionRequest.getMayorNames() == null) {
            LOGGER.warn("Filtering out region request with null mayors: " + regionRequest);
            return false;
        }

        for (String mayorName : regionRequest.getMayorNames()) {
            if (!p.matcher(mayorName).matches()) {
                LOGGER.warn("Filtering out region request with invalid mayor(s): " + regionRequest);
                return false;
            }
        }

        return true;
    }

    private Bounds sort(Location l1, Location l2) {
        Location lowerBounds = new Location();
        Location upperBounds = new Location();

        if (l1.getX() <= l2.getX()) {
            lowerBounds.setX(l1.getX());
            upperBounds.setX(l2.getX());
        } else {
            lowerBounds.setX(l2.getX());
            upperBounds.setX(l1.getX());
        }

        if (l1.getY() <= l2.getY()) {
            lowerBounds.setY(l1.getY());
            upperBounds.setY(l2.getY());
        } else {
            lowerBounds.setY(l2.getY());
            upperBounds.setY(l1.getY());
        }

        if (l1.getZ() <= l2.getZ()) {
            lowerBounds.setZ(l1.getZ());
            upperBounds.setZ(l2.getZ());
        } else {
            lowerBounds.setZ(l2.getZ());
            upperBounds.setZ(l1.getZ());
        }

        return new Bounds(lowerBounds, upperBounds);
    }
}
