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

package tecmo

import arcadia._
import arcadia.gfx._
import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

/**
 * The top-level module.
 *
 * This module abstracts the rest of the arcade hardware from MiSTer-specific things (e.g. SDRAM
 * multiplexer) that are not part of the original arcade hardware design.
 *
 * The memory multiplexer runs in the same (fast) clock domain as the SDRAM. The arcade hardware
 * runs in a separate (slow) clock domain, so the multiplexed memory ports (program ROMs, tile ROMs,
 * etc.) must use clock domain crossing.
 *
 * Because the fast clock domain is an integer multiple of the slow clock domain, we can avoid
 * complex clock domain crossing strategies, and instead use a simple data freezer.
 */
class Main extends Module {
  val io = IO(new Bundle {
    /** Video port */
    val video = Output(new VideoIO)
    /** RGB output */
    val rgb = Output(RGB(Config.COLOR_WIDTH.W))
  })

  // Video timing
  val videoTiming = Module(new VideoTiming(Config.videoTimingConfig))
  videoTiming.io.offset := SVec2(0.S, 0.S)
  videoTiming.io.timing <> io.video

  // Video output
  io.rgb := RGB(0.U, 255.U, 0.U)
}

object Main extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/core"),
    Seq(ChiselGeneratorAnnotation(() => new Main))
  )
}
