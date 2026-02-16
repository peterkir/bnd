# Bndtools Repository View - Eclipse P2 Product Support

## Objective
Enhance the bndtools repository and resolution view to display Eclipse P2 Products as synthetic/virtual entries with hierarchical structure.


add required Model Classes (bndtools.core/src/bndtools/model/repo/)
- **RepositoryProduct.java** - Main product node extending RepositoryEntry, implements Actionable
  - Wraps aQute.p2.api.Product
  - Provides title(), tooltip(), actions() methods
  - Exposes included features and included bundles
  
- **ProductFolderNode.java** - Virtual folder node for grouping feature children
  - Three folder types: INCLUDED_FEATURES, REQUIRED_FEATURES, INCLUDED_BUNDLES
  - Each folder contains wrapped items as children
  
- **IncludedFeatureItem.java** - Wrapper for Feature.IncludedFeature
  - Displays: id, version, optional flag, platform filters (os/ws/arch)
  
- **IncludedBundleItem.java** - Wrapper for Feature.Plugin
  - Displays: id, version, platform filters (os/ws/arch), fragment flag, unpack flag

#### Content Provider (RepositoryTreeContentProvider.java)
- Enhanced `getChildren()` to handle RepositoryProduct and ProductFolderNode
- Implemented `getFeatureChildren()` to build feature hierarchy with 3 folder nodes
- Enhanced `getParent()` to track parent relationships for all product classes
- Enhanced `hasChildren()` to recognize RepositoryProduct and FeatureFolderNode
- Enhanced `getRepositoryBundles()` to fetch features from P2 repositories (FeatureProvider)
  - **CRITICAL**: Products are collected FIRST and their IDs tracked in a Set
  - Bundle creation loop excludes products IDs to prevent duplicates
  - This prevents features from being wrapped as RepositoryBundle objects

#### Repositories View (RepositoriesView.java)
- Enhanced `getRepositoryPlugin()` helper method to handle RepositoryProduct
  - **CRITICAL**: RepositoryProduct check MUST come before RepositoryBundle check (both extend RepositoryEntry)
- Add import for RepositoryProduct class

#### Label Provider (RepositoryTreeLabelProvider.java)
- Add display logic for RepositoryProduct with repository qualifier
  - **CRITICAL**: RepositoryProduct check MUST come before RepositoryBundle check (both extend RepositoryEntry)
- Add display logic for ProiductFolderNode with product icon `bndtools.core\resources\unprocessed\icons\eclipse-icon-16x16.png`
- Add display logic for IncludedFeatureItem with feature icon
- Add display logic for IncludedBundleItem with bundle icon
- Enhanced `getText()` for all product classes with exception handling
- Tooltip support via Actionable interface (description fallback to id+version)

#### Drag-and-Drop Support (SelectionDragAdapter.java, RepositoryBundleSelectionPart.java)
- Enhance `SelectionDragAdapter.dragSetData()` to handle RepositoryProdut
  - Creates drag data as "feature:id:version" string
- Enhance `RepositoryBundleSelectionPart.selectionIsDroppable()` to accept RepositoryProduct
- Enhance `RepositoryBundleSelectionPart.handleSelectionDrop()` to process products
  - Creates VersionedClause with "product:id" BSN
  - Adds "product=true" attribute for resolver identification
  - Preserves version information

#### P2 Repository Integration (P2Repository.java)
- Implemente ProductProvider interface in P2Repository class
- Add `getProducts()` method delegating to P2Indexer
- Add `getPropduct(id, version)` method delegating to P2Indexer
- Create ProductProvider interface in biz.aQute.bndlib

#### P2Indexer Enhancement (P2Indexer.java)
- Implement `getProducts()` method to extract features from indexed resources
  - Queries repository for all resources with `type=org.eclipse.equinox.p2.type.product` in identity capability
  - Returns list of parsed products
- Implement `getProduct(id, version)` method for single feature lookup
- Products should be automatically indexed during P2 repository processing (via `processArtifact()` in P2Indexer)
- Product resources include identity capability with `type=org.eclipse.equinox.p2.type.product`

