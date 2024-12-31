import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

import type { SerializedQuery } from '../Query'
import type { TableName, AppSchema } from '../Schema'
import type { SchemaMigrations } from '../Schema/migrations'
import type { RecordId } from '../Model'

import type {
  DatabaseAdapter,
  CachedFindResult,
  CachedQueryResult,
  BatchOperation,
  UnsafeExecuteOperations,
} from '../adapters/type'

export interface Spec extends TurboModule {
    install(): Promise<boolean>;
    /**
   * Initializes a database connection.
   * Resolves a promise with a status object containing a `code` field
   * (`"ok"`, `"schema_needed"`, or `"migrations_needed"`) and optionally a `databaseVersion`.
   */
    initialize: (
        tag: number,
        databaseName: string,
        schemaVersion: number,
        unsafeNativeReuse: boolean,
        promise: Promise<{ code: "ok" | "schema_needed" | "migrations_needed"; databaseVersion?: number }>
    ) => void;

    /**
     * Sets up a database connection with a schema.
     * Resolves the promise once the connection is established.
     */
    setUpWithSchema: (
        tag: number,
        databaseName: string,
        schema: string,
        schemaVersion: number,
        unsafeNativeReuse: boolean,
        promise: Promise<void>
    ) => void;

    /**
     * Sets up a database connection with migrations.
     * Resolves the promise once the connection is established or rejects if there is an error.
     */
    setUpWithMigrations: (
        tag: number,
        databaseName: string,
        migrations: string,
        fromVersion: number,
        toVersion: number,
        unsafeNativeReuse: boolean,
        promise: Promise<void>
    ) => void;

    find(table: TableName<any>, id: RecordId): Promise<CachedFindResult>
    
    query(query: SerializedQuery): Promise<CachedQueryResult>

    queryIds(query: SerializedQuery): Promise<RecordId[]>

    unsafeQueryRaw(query: SerializedQuery): Promise<any[]>

    count(query: SerializedQuery): Promise<number>

    batch(operations: BatchOperation[]): Promise<void>

    unsafeResetDatabase(): Promise<void>

    getLocal(key: string): Promise<string | undefined>
    
    // unsafeGetLocalSynchronously
    unsafeGetLocalSynchronously: (tag: number, key: string) => [string, string] | null;
    
    provideSyncJson(id: number, syncPullResultJson: string): Promise<void>
    
    // getRandomBytes
    getRandomBytes: (count: 256) => number[];
}

export default TurboModuleRegistry.getEnforcing<Spec>('WatermelonDB');
