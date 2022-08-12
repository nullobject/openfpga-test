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
import chisel3._
import tecmo.Config

/** Represents a sprite descriptor. */
class Sprite extends Bundle {
  /** Enable */
  val enable = Bool()
  /** Code */
  val code = UInt(Config.SPRITE_CODE_WIDTH.W)
  /** Size in pixels (8, 16, 32, 64) */
  val size = UInt(Config.SPRITE_SIZE_WIDTH.W)
  /** Color */
  val color = UInt(Config.COLOR_WIDTH.W)
  /** Position */
  val pos = UVec2(9.W)
  /** Horizontal flip */
  val xFlip = Bool()
  /** Vertical flip */
  val yFlip = Bool()
  /** Priority */
  val priority = UInt(4.W)

  /** Number of columns of tiles */
  def cols: UInt = size / 8.U

  /** Number of rows of tiles */
  def rows: UInt = size / 8.U
}

object Sprite {
  /**
   * Decodes a sprite from the given data.
   *
   * {{{
   *  byte   bits       description
   * ------+-7654-3210-+-------------
   *     0 | xxxx ---- | hi code
   *       | ---- -x-- | enable
   *       | ---- --x- | flip y
   *       | ---- ---x | flip x
   *     1 | xxxx xxxx | lo code
   *     2 | ---- --xx | size
   *     3 | xx-- ---- | priority
   *       | --x- ---- | hi position y
   *       | ---x ---- | hi position x
   *       | ---- xxxx | color
   *     4 | xxxx xxxx | lo position y
   *     5 | xxxx xxxx | lo position x
   *     6 | ---- ---- |
   *     7 | ---- ---- |
   * }}}
   *
   * @param data The sprite data.
   */
  def decode(data: Bits): Sprite = {
    val words = Util.decode(data, 6, 8)

    // The least significant bits of the sprite code should be masked, depending on the size of the
    // sprite.
    //
    // For example:
    //
    // * 16x16 sprites mask 2 LSB.
    // * 32x32 sprites mask 4 LSB.
    // * 64x64 sprites mask 6 LSB.
    val maskSize = words(2)(1, 0) << 1

    val sprite = Wire(new Sprite)
    sprite.enable := words(0)(2)
    sprite.code := Util.maskBits(words(0)(7, 4) ## words(1)(7, 0), maskSize)
    sprite.color := words(3)(3, 0)
    sprite.size := 8.U << words(2)(1, 0)
    sprite.pos := UVec2(words(3)(4) ## words(5)(7, 0), words(3)(5) ## words(4)(7, 0))
    sprite.xFlip := words(0)(0)
    sprite.yFlip := words(0)(1)
    sprite.priority := words(3)(7, 6)
    sprite
  }
}
