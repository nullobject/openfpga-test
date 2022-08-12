/*
 *   __   __     __  __     __         __
 *  /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *  \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *   \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *    \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *   ______     ______       __     ______     ______     ______
 *  /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *  \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *   \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *    \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 * https://joshbassett.info
 * https://twitter.com/nullobject
 * https://github.com/nullobject
 *
 * Copyright (c) 2022 Josh Bassett
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package tecmo.gfx

import arcadia._
import arcadia.gfx._
import chisel3._
import chisel3.util._
import tecmo._

/** Graphics Processor */
class GPU extends Module {
  val io = IO(new Bundle {
    /** Flip video flag */
    val flip = Input(Bool())
    /** Debug mode flag */
    val debug = Input(Bool())
    /** Palette RAM port */
    val paletteRam = new PaletteRamIO
    /** Debug ROM port */
    val debugRom = new TileRomIO
    /** Character control port */
    val charCtrl = new LayerCtrlIO
    /** Foreground control port */
    val fgCtrl = new LayerCtrlIO
    /** Background control port */
    // val bgCtrl = new LayerCtrlIO
    /** Sprite control port */
    // val spriteCtrl = new SpriteCtrlIO
    /** Video port */
    val video = Input(new VideoIO)
    /** RGB output */
    val rgb = Output(RGB(Config.COLOR_WIDTH.W))
    /** Program counter (debug) */
    val pc = Input(UInt(16.W))
  })

  // Frame buffer read position
  // val frameBufferPos = {
  //   val x = Mux(io.flip, ~io.video.pos.x, io.video.pos.x)
  //   val y = Mux(io.flip, ~io.video.pos.y, io.video.pos.y)
  //   UVec2(x, y)
  // }

  // The sprite layer renders to a page in the frame buffer. At the same time, the GPU reads pixels
  // from the alternate page, and renders them to the video output.
  //
  // The frame buffer pages are flipped at the end of each frame (i.e. at the rising edge of the
  // VBLANK signal).
  // val frameBuffer = Module(new FrameBuffer(Config.FRAME_BUFFER_ADDR_WIDTH, Config.FRAME_BUFFER_DATA_WIDTH))
  // frameBuffer.io.portB.rd := io.video.displayEnable
  // frameBuffer.io.portB.addr := frameBufferPos.y(7, 0) ## frameBufferPos.x(7, 0)
  // frameBuffer.io.swap := Util.toggle(Util.rising(io.video.vBlank))

  // Sprite processor
  // val spriteProcessor = Module(new SpriteProcessor)
  // spriteProcessor.io.ctrl <> io.spriteCtrl
  // spriteProcessor.io.frameBuffer <> frameBuffer.io.portA
  // spriteProcessor.io.video <> io.video

  // Character processor
  val charProcessor = Module(new LayerProcessor(tileSize = 8, cols = 32, rows = 32))
  charProcessor.io.ctrl <> io.charCtrl
  charProcessor.io.video <> io.video
  charProcessor.io.flip := io.flip

  // Foreground processor
  val fgProcessor = Module(new LayerProcessor(tileSize = 16, cols = 32, rows = 16))
  fgProcessor.io.ctrl <> io.fgCtrl
  fgProcessor.io.video <> io.video
  fgProcessor.io.flip := io.flip

  // Background processor
  // val bgProcessor = Module(new LayerProcessor(tileSize = 16, cols = 32, rows = 16))
  // bgProcessor.io.ctrl <> io.bgCtrl
  // bgProcessor.io.video <> io.video
  // bgProcessor.io.flip := io.flip

  // Debug layer
  val debugLayer = Module(new DebugLayer(4, 1))
  debugLayer.io.tileRom <> io.debugRom
  debugLayer.io.video <> io.video
  debugLayer.io.text := GPU.decodeDigits(io.pc)
  debugLayer.io.pos := UVec2(0.U, 232.U)
  debugLayer.io.color := 1.U

  // Color mixer
  val colorMixer = Module(new ColorMixer)
  colorMixer.io.paletteRam <> io.paletteRam
  // colorMixer.io.spritePriority := frameBuffer.io.portB.dout(9, 8)
  // colorMixer.io.spriteData := frameBuffer.io.portB.dout(7, 0)
  colorMixer.io.spritePriority := 0.U
  colorMixer.io.spriteData := 0.U
  colorMixer.io.charData := charProcessor.io.data
  colorMixer.io.fgData := fgProcessor.io.data
  // colorMixer.io.bgData := bgProcessor.io.data
  colorMixer.io.bgData := 0.U
  colorMixer.io.debugData := Mux(io.debug, debugLayer.io.data, 0.U)

  // Outputs
  io.rgb := colorMixer.io.rgb
}

object GPU {
  /** The width of a tile (pixels) */
  val TILE_WIDTH = 8

  /** The height of a tile (pixels) */
  val TILE_HEIGHT = 8

  /** The number of bit planes per tile */
  val TILE_BIT_PLANES = 4

  /**
   * Decodes a tile row into a sequence of pixel values.
   *
   * @param row The tile row data.
   */
  def decodeTileRow(row: Bits): Vec[Bits] =
    VecInit(Util.decode(row, TILE_WIDTH, row.getWidth / TILE_WIDTH).reverse)

  /**
   * Decodes a hexadecimal value into a sequence of tile indices.
   *
   * @param value The hexadecimal value.
   */
  def decodeDigits(value: UInt): Vec[UInt] = {
    val BITS_PER_DIGIT = 4

    def tileIndex(n: UInt) = MuxCase(0.U, Seq(
      (n >= 0.U && n <= 9.U, n + 16.U(8.W)),
      (n >= 10.U && n <= 15.U, n + 23.U(8.W))
    ))

    val n = value.getWidth / BITS_PER_DIGIT
    val words = Util.decode(value, n, BITS_PER_DIGIT)
    VecInit(words.map(n => tileIndex(n.asUInt)))
  }
}
