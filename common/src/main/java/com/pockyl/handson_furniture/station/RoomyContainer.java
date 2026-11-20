package com.pockyl.handson_furniture.station;

/**
 * A piece of Refurbished's furniture whose container this mod made larger: room where things stand, beyond the slots its
 * menu has ({@code BasicLootBlockEntityMixin}). What Refurbished works out from its container's size alone (its menus'
 * shift-click, the comparator) uses the size Refurbished gave it.
 */
public interface RoomyContainer {
    /** How many slots Refurbished made the container with: the ones its menu shows. */
    int handson$ownSize();
}
