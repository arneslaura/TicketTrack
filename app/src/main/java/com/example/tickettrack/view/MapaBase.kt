package com.example.tickettrack.view

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * Fondo del mapa.
 * tile.openstreetmap.org bloquea apps.
 * CARTO pide una clave (carto.com/basemaps/apikey).
 * Esri World Street Map responde sin clave. La tesela va en orden zoom/y/x.
 */
object MapaBase {
    val fuente: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "EsriStreet",
        0,
        19,
        256,
        "",
        arrayOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex)
        }
    }

    const val ATRIBUCION = "© Esri, HERE, Garmin, OpenStreetMap"
}
