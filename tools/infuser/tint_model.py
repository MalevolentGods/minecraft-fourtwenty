"""Tinting model for the Inspiration Infuser: colourants in a gallon of ultradeep base, mixed with
two-constant Kubelka-Munk over 31 bands (400..700 nm), seen under D65 by the CIE 1931 observer.

Amounts are in oz per gallon (dispensed in 1/128 oz). The can takes CAP oz of colourant.
"""
import json
import math
import warnings

import numpy as np

warnings.filterwarnings('ignore')
import colour

WL = np.arange(400, 701, 10)
CAP = 12.0
UNIT = 1 / 128

_cmfs = colour.MSDS_CMFS['CIE 1931 2 Degree Standard Observer'].copy().align(colour.SpectralShape(400, 700, 10))
_d65 = colour.SDS_ILLUMINANTS['D65'].copy().align(colour.SpectralShape(400, 700, 10))
CMF = _cmfs.values                       # (31, 3)
ILL = _d65.values                        # (31,)
_W = CMF * ILL[:, None]
_N = _W[:, 1].sum()
WHITE_XYZ = _W.sum(0) / _N


def gauss(c, w, a=1.0):
    return a * np.exp(-0.5 * ((WL - c) / w) ** 2)


def edge(c, w, a=1.0, falling=True):
    """An absorption edge: a below c (falling) or above c (rising), soft over w nm."""
    s = 1 / (1 + np.exp((WL - c) / w))
    return a * (s if falling else 1 - s)


# Each colourant: K (absorption) and S (scattering) per oz in a gallon.
# Shapes follow real pigment families; strengths set how far one shot moves the mix.
COLOURANTS = {
    'white':        dict(K=0.0002 + edge(405, 6, 0.001), S=np.full(31, 6.0)),
    'black':        dict(K=np.full(31, 30.0), S=np.full(31, 0.05)),
    'lemon':        dict(K=edge(484, 8, 30.0) + 0.0012, S=np.full(31, 0.15)),
    'gold':         dict(K=edge(515, 11, 12.0) + edge(600, 70, 0.25) + 0.003, S=np.full(31, 0.2)),
    'orange':       dict(K=edge(546, 10, 14.0) + 0.002, S=np.full(31, 0.15)),
    'scarlet':      dict(K=edge(598, 9, 20.0) + 0.002, S=np.full(31, 0.12)),
    'crimson':      dict(K=gauss(542, 28, 24.0) + edge(420, 10, 1.5) + 0.0015, S=np.full(31, 0.1)),
    'violet':       dict(K=gauss(570, 38, 26.0) + 0.02, S=np.full(31, 0.08)),
    'ultramarine':  dict(K=edge(540, 20, 9.0, falling=False) + 0.004, S=np.full(31, 0.12)),
    'phthalo_blue': dict(K=edge(565, 16, 40.0, falling=False) + edge(420, 9, 4.0) + 0.006, S=np.full(31, 0.06)),
    'phthalo_green': dict(K=edge(472, 12, 30.0) + edge(570, 16, 34.0, falling=False) + 0.006, S=np.full(31, 0.06)),
    'oxide_brown':  dict(K=7.0 * np.exp(-(WL - 400) / 120) + 0.35, S=np.full(31, 0.6)),
}
NAMES = list(COLOURANTS)
KM = np.array([COLOURANTS[n]['K'] for n in NAMES])      # (n, 31)
SM = np.array([COLOURANTS[n]['S'] for n in NAMES])
BASE_K = np.full(31, 0.002)                              # the pool: nearly clear
BASE_S = np.full(31, 0.012)


def reflectance(amounts):
    """amounts: (n,) oz per gallon, in NAMES order."""
    K = BASE_K + amounts @ KM
    S = BASE_S + amounts @ SM
    ks = K / S
    return 1 + ks - np.sqrt(ks * ks + 2 * ks)


def xyz(R):
    return (R[:, None] * _W).sum(0) / _N


_WXY = colour.XYZ_to_xy(WHITE_XYZ)


def lab(R):
    return colour.XYZ_to_Lab(xyz(R), _WXY)


def srgb(R):
    rgb = colour.XYZ_to_sRGB(xyz(R))
    return np.clip(rgb, 0, 1)


def hex_of(R):
    return '#%02X%02X%02X' % tuple(int(round(v * 255)) for v in srgb(R))


def lab_of_hex(h):
    rgb = np.array([int(h[i:i + 2], 16) / 255 for i in (1, 3, 5)])
    return colour.XYZ_to_Lab(colour.sRGB_to_XYZ(rgb), _WXY)


def de(l1, l2):
    return float(colour.delta_E(l1, l2, method='CIE 2000'))


VANILLA = {  # DyeColor texture diffuse colours, MC 1.21
    'white': '#F9FFFE', 'orange': '#F9801D', 'magenta': '#C74EBD', 'light_blue': '#3AB3DA',
    'yellow': '#FED83D', 'lime': '#80C71F', 'pink': '#F38BAA', 'gray': '#474F52',
    'light_gray': '#9D9D97', 'cyan': '#169C9C', 'purple': '#8932B8', 'blue': '#3C44AA',
    'brown': '#835432', 'green': '#5E7C16', 'red': '#B02E26', 'black': '#1D1D21',
}
