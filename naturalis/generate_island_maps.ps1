Add-Type -AssemblyName System.Drawing

$size = 1024
$cx   = $size / 2   # 512
$cy   = $size / 2   # 512
$R    = 460          # island radius in pixels (~920 blocks diameter)

$outDir = "c:\Users\simon\Documents\Code\Addon\naturalis\common\src\main\resources\assets\naturalis\island"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# ─── Biome colours (must match IslandHeightmap.java colour table) ──────────
# name  => (R, G, B, baseGray, minGray, maxGray)
# gray 0 = Y_MIN(-64), gray 255 = Y_MAX(320), water_level = Y40 => gray ~69
$B = @{
    NATURAL_PLAIN   = @(0x3C,0x8C,0x28,  100,  90,115)
    DENSE_FOREST    = @(0x00,0x64,0x00,  115, 100,135)
    JUNGLE          = @(0x00,0xDC,0x3C,  118, 105,140)
    VOLCANO         = @(0xC8,0x00,0x00,  170, 120,240)
    ENDER_FOREST    = @(0x78,0x50,0xA0,  125, 110,148)
    SNOWY_MOUNTAIN  = @(0xC8,0xB4,0x64,  185, 165,215)
    HIGH_PEAK       = @(0x64,0x64,0x69,  200, 175,230)
    ARID_SAVANNA    = @(0x96,0x64,0x32,   92,  82,108)
    CORAL_WATER     = @(0x00,0xC8,0xC8,   55,  40, 68)
    DEEP_WATER      = @(0x00,0x00,0x96,   40,  28, 58)
    DARK_CAVES      = @(0x3C,0x3C,0x3C,  108,  95,125)
    NATURAL_ECHO    = @(0xB4,0xB4,0xB4,  120, 108,138)
    NATURAL_BEACH   = @(0xF0,0xE8,0xC8,   72,  65, 82)
}

# ─── Layout: angle from screen-right (east), clockwise ──────────────────────
# Screen angle 0 = East, 90 = South, 180 = West, 270 = North
# We want a logical layout where North (270°) has peaks, South has volcano etc.
#
# Sectors (startAngle, sweepAngle, biomeName)
$sectors = @(
    @(270,  30, 'HIGH_PEAK'),        # North
    @(300,  35, 'SNOWY_MOUNTAIN'),   # NNE
    @(335,  30, 'DENSE_FOREST'),     # NE
    @(  5,  30, 'JUNGLE'),           # ENE
    @( 35,  35, 'VOLCANO'),          # East-SE
    @( 70,  40, 'ARID_SAVANNA'),     # SE
    @(110,  30, 'NATURAL_BEACH'),    # SSE
    @(140,  30, 'CORAL_WATER'),      # South
    @(170,  30, 'DEEP_WATER'),       # SSW
    @(200,  35, 'DARK_CAVES'),       # SW
    @(235,  35, 'NATURAL_ECHO'),     # WSW
    @(  0,   0, 'ENDER_FOREST')      # placeholder – filled as remainder
)
# Compute the ENDER_FOREST sector to fill the gap 270..270 going counterclockwise
# Gap: from last sector end back to HIGH_PEAK start
# Last explicit end: 235+35 = 270. HIGH_PEAK starts at 270. No gap. So add ENDER_FOREST NW:
$sectors = @(
    @(270,  28, 'HIGH_PEAK'),
    @(298,  32, 'SNOWY_MOUNTAIN'),
    @(330,  30, 'DENSE_FOREST'),
    @(  0,  30, 'JUNGLE'),
    @( 30,  38, 'VOLCANO'),
    @( 68,  42, 'ARID_SAVANNA'),
    @(110,  28, 'NATURAL_BEACH'),
    @(138,  28, 'CORAL_WATER'),
    @(166,  28, 'DEEP_WATER'),
    @(194,  34, 'DARK_CAVES'),
    @(228,  30, 'NATURAL_ECHO'),
    @(258,  12, 'ENDER_FOREST')      # NNW sliver
)
# Add a second ENDER_FOREST wedge NW (180-228 overlap area)
$sectors += @(@(194,   0, 'ENDER_FOREST'))   # ignore, handle below

# Rebuild clean list
$sectors = @(
    @(270,  28, 'HIGH_PEAK'),
    @(298,  32, 'SNOWY_MOUNTAIN'),
    @(330,  30, 'DENSE_FOREST'),
    @(  0,  30, 'JUNGLE'),
    @( 30,  38, 'VOLCANO'),
    @( 68,  42, 'ARID_SAVANNA'),
    @(110,  28, 'NATURAL_BEACH'),
    @(138,  28, 'CORAL_WATER'),
    @(166,  28, 'DEEP_WATER'),
    @(194,  38, 'DARK_CAVES'),
    @(232,  26, 'ENDER_FOREST'),
    @(258,  12, 'NATURAL_ECHO')
)
# Total: 28+32+30+30+38+42+28+28+28+38+26+12 = 360 ✓

# ─── Helper to create SolidBrush ─────────────────────────────────────────────
function New-Brush($r,$g,$b) {
    New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($r,$g,$b))
}
function New-GrayBrush($v) {
    $iv = [int][Math]::Max(0,[Math]::Min(255,$v))
    New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($iv,$iv,$iv))
}

# ─── Paint BIOME MAP ─────────────────────────────────────────────────────────
$bmBiome = New-Object System.Drawing.Bitmap($size, $size)
$gB = [System.Drawing.Graphics]::FromImage($bmBiome)
$gB.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$gB.Clear([System.Drawing.Color]::Black)

# Natural plain as base fill for whole island
$baseBrush = New-Brush $B['NATURAL_PLAIN'][0] $B['NATURAL_PLAIN'][1] $B['NATURAL_PLAIN'][2]
$gB.FillEllipse($baseBrush, ($cx - $R), ($cy - $R), ($R*2), ($R*2))

# Paint each sector
foreach ($s in $sectors) {
    $start = $s[0]; $sweep = $s[1]; $name = $s[2]
    $c = $B[$name]
    $br = New-Brush $c[0] $c[1] $c[2]
    # Use a larger radius so the pie covers the whole island
    $pr = $R + 20
    $gB.FillPie($br, ($cx - $pr), ($cy - $pr), ($pr*2), ($pr*2), $start, $sweep)
    $br.Dispose()
}

# Central NATURAL_PLAIN hub (inner 22%)
$hubR = [int]($R * 0.22)
$gB.FillEllipse($baseBrush, ($cx - $hubR), ($cy - $hubR), ($hubR*2), ($hubR*2))
$baseBrush.Dispose()

# Beach ring around outer edge (outermost 8% of island radius)
$beachR = [int]($R * 0.92)
$beachRingW = $R - $beachR
$beachBrush = New-Brush $B['NATURAL_BEACH'][0] $B['NATURAL_BEACH'][1] $B['NATURAL_BEACH'][2]
# Draw a thick ring by painting outer ellipse in beach then re-clipping
# Simple: overpaint the outer area with beach, then repaint inner with original colors
# Easier: before finalize, use pixel loop for just the outer ring
# We'll handle this with a pixel pass below.
$beachBrush.Dispose()

$gB.Dispose()

# ─── Paint HEIGHTMAP ─────────────────────────────────────────────────────────
$bmH = New-Object System.Drawing.Bitmap($size, $size)
$gH = [System.Drawing.Graphics]::FromImage($bmH)
$gH.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$gH.Clear([System.Drawing.Color]::Black)

# Base island fill with plains gray
$baseGray = $B['NATURAL_PLAIN'][2]
$gH.FillEllipse((New-GrayBrush $baseGray), ($cx - $R), ($cy - $R), ($R*2), ($R*2))

# Paint height sectors
foreach ($s in $sectors) {
    $start = $s[0]; $sweep = $s[1]; $name = $s[2]
    $midGray = [int](($B[$name][3] + $B[$name][4] + $B[$name][5] * 2) / 4)  # weighted toward max
    $pr = $R + 20
    $gH.FillPie((New-GrayBrush $midGray), ($cx - $pr), ($cy - $pr), ($pr*2), ($pr*2), $start, $sweep)
}

# Central hub slightly elevated
$gH.FillEllipse((New-GrayBrush 105), ($cx - $hubR), ($cy - $hubR), ($hubR*2), ($hubR*2))

$gH.Dispose()

# ─── Per-pixel pass: edge beach ring + noise + volcano peak ──────────────────
Write-Host "Running pixel pass (this may take a moment)..."

$rng = New-Object System.Random(12345)

# Lock bits for fast access
$rectFull = New-Object System.Drawing.Rectangle(0, 0, $size, $size)

$bDataB = $bmBiome.LockBits($rectFull,
    [System.Drawing.Imaging.ImageLockMode]::ReadWrite,
    [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$bDataH = $bmH.LockBits($rectFull,
    [System.Drawing.Imaging.ImageLockMode]::ReadWrite,
    [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)

$stride = $bDataB.Stride
$bytes  = $stride * $size
$arrB   = New-Object byte[] $bytes
$arrH   = New-Object byte[] $bytes

[System.Runtime.InteropServices.Marshal]::Copy($bDataB.Scan0, $arrB, 0, $bytes)
[System.Runtime.InteropServices.Marshal]::Copy($bDataH.Scan0, $arrH, 0, $bytes)

$beachR_color = @($B['NATURAL_BEACH'][0], $B['NATURAL_BEACH'][1], $B['NATURAL_BEACH'][2])
$beachMinG = $B['NATURAL_BEACH'][4]
$beachMaxG = $B['NATURAL_BEACH'][5]

$volcanoGray = 240

for ($py = 0; $py -lt $size; $py++) {
    for ($px = 0; $px -lt $size; $px++) {
        $dx = $px - $cx
        $dy = $py - $cy
        $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)

        # Add sine noise to island edge for organic shape
        $angle = [Math]::Atan2($dy, $dx)
        $edgeNoise = 18 * [Math]::Sin(4.1 * $angle) +
                     12 * [Math]::Sin(7.7 * $angle) +
                      8 * [Math]::Sin(13.3 * $angle) +
                      5 * [Math]::Sin(19.7 * $angle)
        $effR = $R + $edgeNoise

        $idx = ($py * $stride) + ($px * 3)

        if ($dist -gt $effR) {
            # Void
            $arrB[$idx]   = 0; $arrB[$idx+1] = 0; $arrB[$idx+2] = 0
            $arrH[$idx]   = 0; $arrH[$idx+1] = 0; $arrH[$idx+2] = 0
        } else {
            $norm = $dist / $effR   # 0 = center, 1 = edge

            # ── Beach ring (outer 9%) ──
            if ($norm -gt 0.91) {
                $arrB[$idx]   = $beachR_color[0]
                $arrB[$idx+1] = $beachR_color[1]
                $arrB[$idx+2] = $beachR_color[2]
                $g = [int]($beachMinG + ($beachMaxG - $beachMinG) * (1.0 - $norm) / 0.09)
                $g = [int][Math]::Max($beachMinG, [Math]::Min($beachMaxG, $g))
                $arrH[$idx] = $g; $arrH[$idx+1] = $g; $arrH[$idx+2] = $g
            } else {
                # Add height noise (+/- 8 gray levels) using sine
                $noiseH = [int](6 * [Math]::Sin(0.08 * $px) * [Math]::Cos(0.07 * $py) +
                                4 * [Math]::Sin(0.15 * $px + 1.3) * [Math]::Cos(0.13 * $py + 0.7))
                $h = $arrH[$idx] + $noiseH
                $h = [int][Math]::Max(0, [Math]::Min(255, $h))
                $arrH[$idx] = $h; $arrH[$idx+1] = $h; $arrH[$idx+2] = $h

                # ── Volcano central peak: boost gray toward center of VOLCANO sector ──
                $degAngle = $angle * 180.0 / [Math]::PI
                if ($degAngle -lt 0) { $degAngle += 360 }
                # VOLCANO sector: startAngle=30, sweep=38 → center≈49° from screen-right
                $volcCenterAngle = 49.0   # screen angle (east=0)
                $angleDiff = [Math]::Abs($degAngle - $volcCenterAngle)
                if ($angleDiff -gt 180) { $angleDiff = 360 - $angleDiff }
                if ($angleDiff -lt 22 -and $norm -gt 0.25 -and $norm -lt 0.72) {
                    # Volcano cone: peak at norm~0.5
                    $coneT = 1.0 - [Math]::Abs($norm - 0.48) / 0.22
                    $coneT = [Math]::Max(0.0, $coneT)
                    $peakBoost = [int](80 * $coneT * (1.0 - $angleDiff / 22.0))
                    $vh = $arrH[$idx] + $peakBoost
                    $vh = [int][Math]::Min(248, $vh)
                    $arrH[$idx] = $vh; $arrH[$idx+1] = $vh; $arrH[$idx+2] = $vh
                }

                # ── HIGH_PEAK / SNOWY_MOUNTAIN ridge boost ──
                $peakCenterAngle = 284.0  # between HIGH_PEAK(270+14) and SNOWY(298+16)=314 → ~284
                $peakDiff = [Math]::Abs($degAngle - $peakCenterAngle)
                if ($peakDiff -gt 180) { $peakDiff = 360 - $peakDiff }
                if ($peakDiff -lt 30 -and $norm -gt 0.3 -and $norm -lt 0.75) {
                    $ridgeT = (1.0 - $peakDiff / 30.0) * (1.0 - [Math]::Abs($norm - 0.52) / 0.22)
                    $ridgeT = [Math]::Max(0.0, $ridgeT)
                    $ridgeBoost = [int](40 * $ridgeT)
                    $rh = $arrH[$idx] + $ridgeBoost
                    $rh = [int][Math]::Min(240, $rh)
                    $arrH[$idx] = $rh; $arrH[$idx+1] = $rh; $arrH[$idx+2] = $rh
                }
            }
        }
    }
}

[System.Runtime.InteropServices.Marshal]::Copy($arrB, 0, $bDataB.Scan0, $bytes)
[System.Runtime.InteropServices.Marshal]::Copy($arrH, 0, $bDataH.Scan0, $bytes)
$bmBiome.UnlockBits($bDataB)
$bmH.UnlockBits($bDataH)

# ─── Save ─────────────────────────────────────────────────────────────────────
$bmBiome.Save("$outDir\biome_map.png",  [System.Drawing.Imaging.ImageFormat]::Png)
$bmH.Save(    "$outDir\heightmap.png",  [System.Drawing.Imaging.ImageFormat]::Png)

$bmBiome.Dispose()
$bmH.Dispose()

Write-Host "Done. Saved to: $outDir"
Write-Host "  biome_map.png  (1024x1024)"
Write-Host "  heightmap.png  (1024x1024)"
