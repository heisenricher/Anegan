/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * Nova Design Token System — V3.2
 *
 * Centralized design tokens for the entire Anegan app.
 * Every visual property references these tokens instead of hardcoded values.
 * Based on an 8pt base grid with futuristic Neo-Brutalist aesthetics.
 */
object NovaTokens {

    // ═══════════════════════════════════════════
    // SPACING — 8pt Base Grid
    // ═══════════════════════════════════════════
    object Spacing {
        val none   = 0.dp       // No spacing
        val xxxs   = 2.dp       // Hairline gaps
        val xxs    = 4.dp       // Icon-to-label gaps, badge offsets
        val xs     = 8.dp       // Tight internal padding
        val sm     = 12.dp      // Card internal padding, list item spacing
        val md     = 16.dp      // Section spacing, standard padding
        val lg     = 20.dp      // Component spacing, group margins
        val xl     = 24.dp      // Section margins, page horizontal padding
        val xxl    = 32.dp      // Page margins, hero spacing
        val xxxl   = 40.dp      // Dramatic spacing, section separators
        val mega   = 56.dp      // Full-bleed spacing, toolbar height
        val ultra  = 72.dp      // Bottom content padding for FAB clearance
    }

    // ═══════════════════════════════════════════
    // CORNER RADIUS — Graduated Scale
    // ═══════════════════════════════════════════
    object Radius {
        val none   = 0.dp       // Sharp corners
        val xs     = 4.dp       // Small badges, chips
        val sm     = 8.dp       // Buttons, inputs, small cards
        val md     = 12.dp      // Medium cards, dialogs
        val lg     = 16.dp      // Standard cards, panels
        val xl     = 20.dp      // Large cards, category sections
        val xxl    = 24.dp      // Premium cards, hero cards
        val xxxl   = 32.dp      // Bottom sheets, modals
        val pill   = 999.dp     // Full pill / capsule shape
    }

    // ═══════════════════════════════════════════
    // ELEVATION — Spatial Depth Layers
    // ═══════════════════════════════════════════
    object Elevation {
        val flat   = 0.dp       // Background-level elements
        val subtle = 1.dp       // Barely raised — substrate layer
        val low    = 2.dp       // Standard cards, list items
        val medium = 4.dp       // Elevated cards, selected items
        val high   = 8.dp       // Floating elements, FABs
        val mega   = 12.dp      // Modals, bottom sheets
        val ultra  = 24.dp      // Tooltips, dropdown overlays
    }

    // ═══════════════════════════════════════════
    // BLUR — Glassmorphism Radii (Float for RenderEffect)
    // ═══════════════════════════════════════════
    object Blur {
        val none       = 0f     // No blur
        val subtle     = 8f     // Hint of frosted glass
        val light      = 16f    // Soft frosted glass
        val medium     = 24f    // Standard glass card effect
        val heavy      = 32f    // Dense frosted glass
        val ultra      = 48f    // Maximum blur — modals, overlays
        val background = 64f    // Background substrate blur
    }

    // ═══════════════════════════════════════════
    // OPACITY — Alpha Values
    // ═══════════════════════════════════════════
    object Opacity {
        val invisible = 0.00f   // Fully transparent
        val ghost     = 0.04f   // Barely visible tints
        val whisper   = 0.08f   // Subtle background tints, borders
        val faint     = 0.12f   // Dividers, inactive borders
        val soft      = 0.20f   // Overlay backgrounds, glass fills
        val medium    = 0.40f   // Secondary text on glass
        val strong    = 0.60f   // Primary text on glass, active borders
        val heavy     = 0.80f   // Dense overlays, scrims
        val opaque    = 1.00f   // Fully opaque
    }

    // ═══════════════════════════════════════════
    // TOUCH — Minimum Interactive Target Sizes
    // ═══════════════════════════════════════════
    object Touch {
        val minimum     = 48.dp  // WCAG/Material minimum touch target
        val comfortable = 56.dp  // Calculator keys, utility buttons
        val large       = 64.dp  // Primary CTAs, widget icons
        val hero        = 72.dp  // Hero action buttons, record button
        val mega        = 80.dp  // Full-screen toggle (flashlight)
    }

    // ═══════════════════════════════════════════
    // ANIMATION — Duration (ms) & Spring Configurations
    // ═══════════════════════════════════════════
    object Motion {
        // Durations (milliseconds)
        val instant   = 100     // Micro-feedback (color shift, alpha change)
        val fast      = 200     // Small element transitions, chip select
        val normal    = 300     // Standard transitions, card enter/exit
        val slow      = 500     // Page transitions, screen changes
        val dramatic  = 800     // Hero animations, splash effects
        val cinematic = 1200    // Onboarding, first-launch sequences

        // Stagger delay between sequential items (ms)
        val staggerDelay = 30

        // Spring Configurations
        val springSnappy = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 800f
        )
        val springBouncy = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 400f
        )
        val springSmooth = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 300f
        )
        val springGentle = spring<Float>(
            dampingRatio = 0.9f,
            stiffness = 200f
        )

        // Press Scale Values
        val pressScaleWidget = 0.88f   // Deep bounce for dashboard widgets
        val pressScaleCard   = 0.95f   // Subtle press for cards
        val pressScaleButton = 0.92f   // Medium press for buttons
    }

    // ═══════════════════════════════════════════
    // ICON — Size Scale
    // ═══════════════════════════════════════════
    object IconSize {
        val xxs  = 12.dp   // Badge indicators, status dots
        val xs   = 16.dp   // Inline small indicators, chevrons
        val sm   = 20.dp   // Secondary action icons, chips
        val md   = 24.dp   // Standard toolbar/list icons
        val lg   = 28.dp   // Emphasized icons, widget icons
        val xl   = 32.dp   // Card header icons, section icons
        val xxl  = 40.dp   // Hero icons, empty state icons
        val mega = 56.dp   // Dashboard widget icons (large)
    }

    // ═══════════════════════════════════════════
    // WIDGET — Dashboard Grid Configuration
    // ═══════════════════════════════════════════
    object Widget {
        val iconContainerSize = 64.dp   // Neon ring circle (was 52dp)
        val iconSize          = 28.dp   // Icon inside circle (was 24dp)
        val badgeSize         = 20.dp   // Status badge (was 16dp)
        val labelMaxWidth     = 80.dp   // Max label width (was 72dp)
        val gridColumns       = 4       // Tools per row (was 3)
        val gridSpacing       = 12.dp   // Gap between grid items
        val sectionCorner     = 24.dp   // Section card corner radius (was 20dp)
        val sectionPadding    = 16.dp   // Internal section padding (was 14dp)
    }

    // ═══════════════════════════════════════════
    // GLASS — Glassmorphism Card Properties
    // ═══════════════════════════════════════════
    object Glass {
        val borderWidth     = 1.dp      // Glass card border thickness
        val neonBorderAlpha = 0.30f     // Neon accent border opacity
        val fillAlphaLight  = 0.06f     // Glass fill on light backgrounds
        val fillAlphaDark   = 0.12f     // Glass fill on dark backgrounds
        val glowRadius      = 0.6f      // Glow effect radius multiplier
        val glowAlphaIdle   = 0.15f     // Resting glow intensity
        val glowAlphaActive = 0.40f     // Active/pressed glow intensity
    }

    // ═══════════════════════════════════════════
    // NEON — Neon Ring & Glow Properties
    // ═══════════════════════════════════════════
    object Neon {
        val ringStrokeWidth   = 2.dp    // Neon ring border thickness
        val ringPadding       = 8.dp    // Gap between icon and neon ring
        val pulseMinAlpha     = 0.15f   // Idle neon ring minimum alpha
        val pulseMaxAlpha     = 0.25f   // Idle neon ring maximum alpha
        val pulseDurationMs   = 4000    // Full pulse cycle duration
        val pressedAlpha      = 0.80f   // Neon ring alpha when pressed
        val activeAlpha       = 0.50f   // Neon ring alpha for active states
    }
}
