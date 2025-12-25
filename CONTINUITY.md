# Continuity Ledger - Famy Family Tree App Enhancement

## Goal (incl. success criteria)
Transform Famy into a production-grade, performance-optimized family tree management app with:
1. **Fix FOREIGN KEY constraint error** - When adding family members (visible in screenshot error)
2. **Redesign Add Member screen** - Enhanced fields (middle name, education, interests, career status, etc.) with presets
3. **Fix Location Search** - Make Nominatim/Photon APIs work properly with debouncing and caching
4. **High-Performance Tree Canvas** - Viewport culling, virtualization, lazy loading for 100+ members
5. **Modern UI/UX** - Minimal, responsive, professional design throughout
6. **Production-grade code** - Modular (500-1000 lines/file max), no TODOs

Success criteria: Zero bugs, smooth performance on low-end devices, professional UI/UX.

## Constraints/Assumptions
- Kotlin + Jetpack Compose (Material 3)
- Offline-first (Room database)
- Free map APIs only (Nominatim, Photon)
- Modular code: 500-1000 lines per file max
- No TODOs or placeholder implementations
- All changes production-grade, fully functional

## Key Decisions
- **FK Fix**: The error occurs because createMemberUseCase creates member, then updateMemberUseCase is called again unnecessarily
- **Map APIs**: Dual Nominatim + Photon approach already in codebase, needs fixing
- **Tree Rendering**: Canvas-based with viewport culling exists, needs profile photo optimization
- **Schema**: Already has middleName, education, interests, careerStatus, relationshipStatus - just needs UI enhancement

## State

### Done
- Codebase analysis complete
- Root cause of FK error identified: In EditMemberViewModel.save(), after createMemberUseCase creates member, updateMemberUseCase is called with full member data, potentially causing FK issues

### Now
- Fixing the FK constraint error in member creation flow

### Next (Priority Order)
1. Fix FK error in EditMemberViewModel member creation
2. Enhance EditMemberScreen UI/UX with new fields and better design
3. Fix location search with proper debouncing
4. Optimize TreeCanvas rendering and profile pictures
5. Performance optimization throughout

## Open Questions
- None currently

## Working Set
- `/ui/screen/editor/EditMemberViewModel.kt` - FK fix needed (line 542)
- `/ui/screen/editor/EditMemberScreen.kt` - UI enhancement
- `/ui/screen/tree/component/TreeCanvas.kt` - Performance optimization
- `/data/remote/LocationServiceImpl.kt` - Location search
- `/domain/usecase/MemberUseCases.kt` - Member creation logic
