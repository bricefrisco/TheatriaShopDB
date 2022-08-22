package com.playtheatria.shopdb.models.players;

import com.playtheatria.shopdb.database.Player;
import com.playtheatria.shopdb.database.Region;

import java.util.ArrayList;
import java.util.List;

public final class PlayerMapper {
    public static PlayerRegionDto toPlayerRegionDto(Region region) {
        if (region == null) {
            return null;
        }

        PlayerRegionDto result = new PlayerRegionDto();
        result.setName(region.getName());
        result.setServer(region.server);

        return result;
    }

    public static PlayerDto toPlayerDto(Player player) {
        if (player == null) {
            return null;
        }

        PlayerDto result = new PlayerDto();
        result.setName(player.getName());
        result.setNumChestShops(player.chestShops.size());

        List<PlayerRegionDto> towns = new ArrayList<>();
        if (player.towns != null) {
            for (Region town : player.towns) {
                PlayerRegionDto playerRegionDto = toPlayerRegionDto(town);
                if (playerRegionDto != null) {
                    towns.add(playerRegionDto);
                }
            }
        }
        result.setTowns(towns);

        return result;
    }
}
