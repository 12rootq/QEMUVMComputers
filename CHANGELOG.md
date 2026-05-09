# Changelog

## [1.4.16-1.20.1-fabric] - 2025-05-09

### Security & Critical Bug Fixes

- **Fixed UUID mismatch in multiplayer screen texture cleanup** (`ClientMod.java`)
  - `S2C_STOP_SCREEN` and `S2C_SCREEN` were using `mcc.player.getUuid()` for `remove()` instead of `pcOwner`.
  - This caused screen textures from other players to **never be destroyed**, leading to a severe **memory leak** (`NativeImage`, `NativeImageBackedTexture`, `Identifier`).
  - Also fixed a **double-close bug** where `vmScreenTextureNI.close()` was called instead of `vmScreenTextureNIBT.close()` in `S2C_SCREEN`.

- **Fixed component loss when placing last PC case from stack** (`ItemPCCase.java`, `ItemPCCaseSidepanel.java`)
  - `decrement(1)` was called **before** reading NBT data. When `count == 1`, the stack became `EMPTY` and `getNbt()` returned `null`, causing **permanent loss** of all PC components (motherboard, CPU, RAM, HDD, ISO).
  - Now NBT is saved to a local variable **before** decrementing.

- **Fixed server crash on missing inventory item** (`MainMod.java`)
  - `removeStck()` threw `RuntimeException` when an item was not found, crashing the **entire server**.
  - Replaced with logging via `LOGGER.error()` and graceful return.
  - Also hardened against desync between `contains()` check and `removeStck()` execution.

- **Fixed ClassCastException exploit in C2S_ORDER** (`MainMod.java`)
  - No validation that `readItemStack().getItem()` is actually an `OrderableItem`. A malicious client could send any item, causing a server crash.
  - Added `instanceof OrderableItem` check with logging of invalid packets.
  - Same fix applied to `S2C_SYNC_ORDER` handler in `ClientMod.java`.

- **Fixed IllegalArgumentException in C2S_ADD_CPU / C2S_ADD_RAM** (`MainMod.java`)
  - When `dividedBy` or `mb` values did not match expected constants, `lookingFor` remained `null`, causing `new ItemStack(null)` → server crash.
  - Added `lookingFor != null` guard before creating `ItemStack`.

- **Fixed HDD removal ignoring NBT** (`MainMod.java`)
  - `removeStck()` matched only by `Item` type, not NBT. If a player had multiple HDDs with different VHD files, the **wrong disk** could be removed.
  - Enhanced `removeStck()` to prefer **exact NBT match** before falling back to type-only match.

- **Fixed VM always created with only 1 CPU** (`GuiPCEditing.java`)
  - `Math.min(1, ...)` in the "create new VM" branch always returned `1`, regardless of host CPU count or player settings. The "modify existing" branch already used correct `Math.max(1, ...)`.
  - Fixed to `Math.max(1, ...)` — VM now gets the correct number of CPUs.

- **Fixed ConcurrentModificationException in ServerMixin** (`ServerMixin.java`)
  - `MainMod.orders.remove()` was called during `for-each` iteration over `HashMap.values()`, guaranteeing a crash every time an order completed.
  - Implemented deferred removal: collect keys to remove in a list, then delete after the loop.

- **Fixed NPE when sending packets to disconnected players** (`ServerMixin.java`)
  - `playerManager.getPlayer()` could return `null` if a player logged out. Calling `.getWorld()` or `ServerPlayNetworking.send(null, ...)` caused NPE.
  - Added null checks before all `getPlayer()` usages.

- **Fixed deadlock in insertISO** (`GuiPCEditing.java`)
  - `wait()` without `while` loop and without timeout — if `notify()` was called before `wait()`, the thread hung forever.
  - Replaced with `while (condition) { lock.wait(5000); }` pattern using a shared `ClientMod.VM_TURNING_ON_LOCK`.
  - Also fixed the case where a new `GuiPCEditing` instance had a different monitor object than the one `notify()` was called on.

- **Fixed busy-wait burning 100% CPU in turnOffPC** (`GuiPCEditing.java`)
  - `while(ClientMod.vmTurningOn) {}` was an infinite spin-loop.
  - Replaced with `Thread.sleep(50)` inside the loop. Also added `volatile` to `vmTurningOn` so the thread actually sees state changes.

- **Fixed Timer thread leak in GuiFocus** (`GuiFocus.java`)
  - A new `Timer` was created every time `GuiFocus` opened, but `cancel()` was only called on `NullPointerException` (singleplayer). In multiplayer, timers leaked indefinitely.
  - Timer is now stored as a field and cancelled in `removed()`.

- **Fixed NPE-based control flow in singleplayer** (`GuiFocus.java`)
  - `getCurrentServerEntry().address` throws NPE in singleplayer. The code used `catch(NullPointerException)` as control flow.
  - Replaced with explicit `== null` check.

- **Fixed resource leak in VBoxManage** (`VBoxManage.java`)
  - `BufferedReader` and `InputStream` were not closed in `finally`, leaking file descriptors on exceptions.
  - Wrapped in `try-with-resources`.

- **Fixed FileWriter not closed on exception** (`ClientMod.java`)
  - `increaseVHDNum()` opened a `FileWriter` without `try-with-resources` or `finally`. On any exception, the file handle leaked.
  - Wrapped in `try-with-resources`.

- **Fixed Unpooled.buffer() leaks across all packet handlers**
  - `new PacketByteBuf(Unpooled.buffer())` was used in `ClientMod.java`, `GuiPCEditing.java`, `ServerMixin.java`, and `PlayerManagerMixin.java` instead of `PacketByteBufs.create()`.
  - Replaced all instances with the proper Fabric API method.

- **Fixed regex bug in hard drive selection** (`GuiCreateHarddrive.java`)
  - `split(" | ")` treated `" | "` as a regex (`space OR space`), effectively equivalent to `split(" ")`.
  - Any VHD filename containing a space was broken. Replaced with `indexOf(" | ")` + `substring()` for literal matching.

- **Fixed iron ingot overpayment loss** (`EntityDeliveryChest.java`)
  - When a player overpaid, `is.increment()` was called on an already-empty `ItemStack` (after `decrement()`), causing the **excess ingots to be permanently lost**.
  - Fixed by saving the count before `decrement()`, then creating a new `ItemStack` for the refund and using `player.getInventory().offerOrDrop()`.

- **Fixed matrix stack imbalance on exception** (`HeldItemMixin.java`)
  - `matrices.push()` in HEAD and `matrices.pop()` in TAIL were not atomic. An exception between them left the stack unbalanced.
  - Wrapped rendering in `try-finally` to guarantee `matrices.pop()`.

- **Fixed NBT key inconsistency** (`EntityPC.java`, `ItemPCCase.java`, `ItemPCCaseSidepanel.java`)
  - Constructor used different NBT keys (`x64`, `MoboInstalled`, `GPUInstalled`, `RAMSlot0`, `VHDName`, `ISOName`) than `readCustomDataFromNbt` / `writeCustomDataToNbt` (`X64`, `MotherboardInstalled`, `GpuInstalled`, `GbRamSlot0`, `HardDriveFileName`, `IsoFileName`).
  - This worked only because the two paths never crossed, but was a refactoring minefield.
  - Unified all keys to match `writeCustomDataToNbt` format. Added **backward compatibility** in constructor to read both old and new keys.

### New Features

- **Transition to VBoxManage CLI (VBox CLI)**
  - Replaced the deprecated **VirtualBox JWS (Java Web Services) API** with a new `VBoxManage.java` wrapper that calls the `VBoxManage` command-line tool directly.
  - The JWS API was **removed in VirtualBox 7.0+**, making the old mod completely incompatible with modern VirtualBox installations.
  - All VM operations are now performed via CLI:
    - `createVm`, `modifyVm`, `startVm`, `powerOffVm`
    - `storageAttach`, `addStorageController`, `removeStorageController`
    - `mountMedium`, `unmountMedium`
    - `putScancodes`, `putMouseEvent`
    - `takeScreenshot`
    - `getVmState`, `vmExists`, `discardSavedState`
  - Added `executeSilent()` for optional operations that may fail on newer VBox versions (e.g., `--accelerate2dvideo` in VBox 7.x).
  - Added connection test (`testConnection()`) and version detection (`getVersion()`).

- **Support for VirtualBox 7.x and newer**
  - VBox 7.0+ removed `vboxwebsrv` (the SOAP web service). The mod now works with **any VirtualBox version** that provides `VBoxManage`.
  - Added parsing of `showvminfo --machinereadable` output for medium attachment state, compatible with VBox 7.x output format.
  - `--accelerate2dvideo` is now handled silently (ignored if unsupported) instead of crashing.

### Performance Improvements

- **Added `volatile` to all cross-thread shared state** (`ClientMod.java`)
  - `vmTurnedOn`, `vmTurningOn`, `vmTurningOff`, `vmUpdateThread`, `vmTextureBytes`, `vmTextureBytesSize`.
  - Prevents infinite busy-waits and race conditions between the render thread and VM update thread.

- **Added `synchronized` to texture byte transfers** (`VMRunnable.java` + `ClientMod.java`)
  - `vmTextureBytes` / `vmTextureBytesSize` were updated in one thread and read in another without any synchronization, risking torn reads.
  - Added `VM_TEXTURE_LOCK` with proper `synchronized` blocks on both write (VM thread) and read (render thread) sides.

- **Added `synchronized` to tablet texture streaming** (`TabletOS.java`)
  - `byteArrayInputStream` was written by the tablet renderer thread and read by the main render thread without synchronization.
  - Added `TEXTURE_SYNC` lock with local copy pattern.

- **Throttled `isMediumEjected()` polling** (`GuiPCEditing.java`)
  - Previously called `VBoxManage showvminfo` **every frame (~60 times/second)**, spawning a new OS process each time.
  - Now checks **every 2 seconds**, reducing CPU and disk load by ~99%.

- **Throttled tablet renderer** (`GameloopMixin.java`)
  - Tablet renderer `while(true)` loop had no `Thread.sleep()`, pinning a CPU core at 100%.
  - Added `Thread.sleep(33)` for ~30 FPS cap.
  - Also replaced silent `ConcurrentModificationException` swallowing with proper logging.

- **Reduced S2C_SYNC_ORDER network spam** (`ServerMixin.java`)
  - Order sync packet was sent **20 times/second per order** regardless of whether status changed.
  - Now tracks `lastSyncedStatus` and only sends the packet when the status actually changes.

### Architecture & Code Quality

- **Proper use of `PacketByteBufs.create()` throughout**
  - Eliminates manual buffer lifecycle management and potential leaks.

- **Added SLF4J/Log4j logging** (`MainMod.java`)
  - Replaced `RuntimeException` server crashes with `LOGGER.error()` for inventory desync events.

- **Backward compatibility for old NBT saves**
  - Old PC cases placed in the world before this update will still load correctly thanks to dual-key reading in `EntityPC` constructor.

---

## [1.4.15-1.20.1-fabric] - 2026-04-08

### Added
- Ported the mod to Minecraft **1.20.1**.
- Full support for **Fabric Loader**.
- Updated to **Java 17**.

### Changed
- Updated all dependencies to be compatible with 1.20.1 (Fabric API, Yarn mappings).
- Refactored rendering logic to comply with modern Minecraft rendering standards.
- Updated entity interaction logic to ensure compatibility with 1.20.1.
- Integrated ShadowJar for better dependency management and to resolve class loading issues (e.g., `CatalogManager`).

### Fixed
- Fixed GUI rendering issues that were present during the porting process.
- Fixed an issue where the PC editing menu would not open when interacting with components.
- Resolved various runtime compatibility errors related to Java 17 and relocated libraries.

## Development Plans (Branches)

Three main development branches have been defined for the project:

1. **Legacy**:
   - Contains already released versions and versions that have become obsolete for any reason (critical bugs, etc.).
2. **Port/Rewrite**:
   - Port of the original mod and its refinement as intended by the original author.
   - Versions: **1.x.x**.
3. **Reimagining**:
   - Personal ideas, custom components, new devices, and expanded functionality.
   - Versions: **2.x.x**.
