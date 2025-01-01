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
    install(successCallback: (result: boolean) => void, errorCallback: (error: string) => void): void;
    
    initialize: (
      tag: number,
      databaseName: string,
      schemaVersion: number,
      unsafeNativeReuse: boolean,
      successCallback: (result: { code: "ok" | "schema_needed" | "migrations_needed"; databaseVersion?: number }) => void,
      errorCallback: (error: string) => void
    ) => void;

    setUpWithSchema: (
      tag: number,
      databaseName: string,
      schema: string,
      schemaVersion: number,
      unsafeNativeReuse: boolean,
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ) => void;

    setUpWithMigrations: (
      tag: number,
      databaseName: string,
      migrations: string,
      fromVersion: number,
      toVersion: number,
      unsafeNativeReuse: boolean,
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ) => void;

    find(
      table: TableName<any>,
      id: RecordId,
      successCallback: (result: CachedFindResult) => void,
      errorCallback: (error: string) => void
    ): void;
    
    query(
      query: SerializedQuery,
      successCallback: (result: CachedQueryResult) => void,
      errorCallback: (error: string) => void
    ): void;

    queryIds(
      query: SerializedQuery,
      successCallback: (result: RecordId[]) => void,
      errorCallback: (error: string) => void
    ): void;
  
    unsafeQueryRaw(
      query: SerializedQuery,
      successCallback: (result: any[]) => void,
      errorCallback: (error: string) => void
    ): void;

    count(
      query: SerializedQuery,
      successCallback: (result: number) => void,
      errorCallback: (error: string) => void
    ): void;

    batch(
      operations: BatchOperation[],
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ): void;

    unsafeResetDatabase(
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ): void;

    unsafeGetLocalSynchronously(
      tag: number,
      key: string
    ): [string, string] | null;

    provideSyncJson(
      id: number,
      syncPullResultJson: string,
      successCallback: (result: void) => void,
      errorCallback: (error: string) => void
    ): void;
    
    // getRandomBytes
    getRandomBytes: (count: 256) => number[];
}

export default TurboModuleRegistry.getEnforcing<Spec>('WatermelonDB');
