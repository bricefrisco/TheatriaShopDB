package com.playtheatria.shopdb.models.regions;

import com.playtheatria.shopdb.database.Player;
import com.playtheatria.shopdb.database.Region;

import java.util.ArrayList;
import java.util.List;

public final class RegionMapper {
    public static RegionPlayerDto toRegionPlayerDto(Player player) {
        if (player == null) {
            return null;
        }

        RegionPlayerDto result = new RegionPlayerDto();
        result.setName(player.getName());

        return result;
    }

    public static RegionDto toRegionDto(Region region) {
        if (region == null) {
            return null;
        }

        RegionDto result = new RegionDto();
        result.setName(region.getName());
        result.setServer(region.server);
        result.setiBounds(region.getiBounds());
        result.setoBounds(region.getoBounds());
        result.setNumChestShops(region.chestShops.size());
        result.setActive(region.active);

        List<RegionPlayerDto> mayors = new ArrayList<>();
        if (region.mayors != null) {
            for (Player player : region.mayors) {
                RegionPlayerDto regionPlayerDto = toRegionPlayerDto(player);
                if (regionPlayerDto != null) {
                    mayors.add(regionPlayerDto);
                }
            }
        }
        result.setMayors(mayors);

        result.setLastUpdated(region.lastUpdated);

        return result;
    }
}
